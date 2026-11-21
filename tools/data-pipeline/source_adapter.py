import abc
import hashlib
import json
import re
import urllib.request
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional, Tuple, Set

from pipeline_models import (
    NormalizedCategory,
    NormalizedItem,
    NormalizedMachine,
    NormalizedIngredient,
    NormalizedProduct,
    NormalizedRecipe,
    NormalizedMachineRecipe,
    ParsedDataset,
)

class SourceAdapter(abc.ABC):
    """Abstract base adapter for retrieving and parsing game data from diverse remote/local sources."""

    @abc.abstractmethod
    def fetch(self, dataset_or_path: str, custom_url: Optional[str] = None) -> Tuple[Dict[str, Any], str, str]:
        """
        Retrieves raw dataset JSON.
        Returns (parsed_json_dict, resolved_source_url, sha256_checksum).
        """
        pass

    @abc.abstractmethod
    def parse(self, payload: Dict[str, Any], metadata_overrides: Optional[Dict[str, str]] = None) -> ParsedDataset:
        """Transforms source JSON into normalized domain models with reference resolution."""
        pass


def compute_sha256(data_bytes: bytes) -> str:
    return hashlib.sha256(data_bytes).hexdigest()


def parse_energy_to_kw(energy_str: Any) -> float:
    """Converts energy strings like '150kW', '3.5MW', '200J', '10MJ' into kW."""
    if energy_str is None:
        return 0.0
    if isinstance(energy_str, (int, float)):
        return float(energy_str)
    
    s = str(energy_str).strip()
    match = re.match(r"^([\d.]+)\s*([a-zA-Z]*)$", s)
    if not match:
        try:
            return float(s)
        except ValueError:
            return 0.0

    val = float(match.group(1))
    unit = match.group(2).lower()

    if unit in ("w", "j"):
        return val / 1000.0
    elif unit in ("kw", "kj", ""):
        return val
    elif unit in ("mw", "mj"):
        return val * 1000.0
    elif unit in ("gw", "gj"):
        return val * 1000000.0
    return val


class FactorioLabAdapter(SourceAdapter):
    """
    Adapter for factoriolab/factoriolab repository.
    Source pattern: https://raw.githubusercontent.com/factoriolab/factoriolab/main/public/data/{dataset}/data.json
    Supports datasets: 2.0 (Base 2.0), spa (Space Age 2.0), 1.1, etc.
    """

    DEFAULT_BASE_URL = "https://raw.githubusercontent.com/factoriolab/factoriolab/main/public/data/{dataset}/data.json"

    def fetch(self, dataset_or_path: str, custom_url: Optional[str] = None) -> Tuple[Dict[str, Any], str, str]:
        if custom_url:
            url = custom_url
        elif dataset_or_path.startswith("http://") or dataset_or_path.startswith("https://"):
            url = dataset_or_path
        elif dataset_or_path.endswith(".json") and ("://" not in dataset_or_path):
            # Local file path
            with open(dataset_or_path, "rb") as f:
                content_bytes = f.read()
            checksum = compute_sha256(content_bytes)
            return json.loads(content_bytes.decode("utf-8")), f"file://{dataset_or_path}", checksum
        else:
            url = self.DEFAULT_BASE_URL.format(dataset=dataset_or_path)

        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "Factoria-DataPipeline/1.0",
                "Accept": "application/json"
            }
        )
        with urllib.request.urlopen(req, timeout=30) as response:
            if response.status != 200:
                raise RuntimeError(f"HTTP {response.status} when fetching {url}")
            content_bytes = response.read()

        checksum = compute_sha256(content_bytes)
        payload = json.loads(content_bytes.decode("utf-8"))
        return payload, url, checksum

    def parse(self, payload: Dict[str, Any], metadata_overrides: Optional[Dict[str, str]] = None) -> ParsedDataset:
        metadata_overrides = metadata_overrides or {}
        
        # 1. Categories
        categories: List[NormalizedCategory] = []
        category_ids: Set[str] = set()
        for cat in payload.get("categories", []):
            cid = str(cat["id"]).strip()
            name = str(cat.get("name", cid.replace("-", " ").capitalize())).strip()
            icon = cat.get("icon", cid)
            categories.append(NormalizedCategory(id=cid, name=name, icon=icon))
            category_ids.add(cid)

        # Fallback category for unmapped items
        if "unknown" not in category_ids:
            categories.append(NormalizedCategory(id="unknown", name="Uncategorized", icon=None))
            category_ids.add("unknown")

        # 2. Items & Machines
        items: List[NormalizedItem] = []
        machines: List[NormalizedMachine] = []
        item_ids: Set[str] = set()
        machine_ids: Set[str] = set()

        for it in payload.get("items", []):
            iid = str(it["id"]).strip()
            name = str(it.get("name", iid.replace("-", " ").capitalize())).strip()
            cat_id = it.get("category")
            if cat_id not in category_ids:
                cat_id = "unknown"

            # In FactorioLab, fluids have category "fluids" or omit "stack"
            is_fluid = (cat_id == "fluids") or (it.get("type") == "fluid") or ("stack" not in it and "machine" not in it and "pipe" not in it and "belt" not in it)
            itype = "fluid" if is_fluid else "item"
            stack_size = int(it.get("stack", 1 if is_fluid else 50))
            icon = it.get("icon", iid)

            items.append(NormalizedItem(
                id=iid,
                name=name,
                category_id=cat_id,
                stack_size=stack_size,
                icon=icon,
                type=itype
            ))
            item_ids.add(iid)

            # Check if this item is also an entity/machine
            if "machine" in it and isinstance(it["machine"], dict):
                m_info = it["machine"]
                speed = float(m_info.get("speed", 1.0))
                # Passive machines (like offshore pump) consume 0 kW
                usage_kw = float(m_info.get("usage", 0.0))
                pollution = float(m_info.get("pollution", 0.0))
                modules = int(m_info.get("modules", 0))

                machines.append(NormalizedMachine(
                    id=iid,
                    name=name,
                    category=cat_id,
                    crafting_speed=speed,
                    energy_usage_kw=usage_kw,
                    pollution_per_minute=pollution,
                    module_slots=modules,
                    icon=icon
                ))
                machine_ids.add(iid)

        # 3. Recipes, Ingredients, Products & Machine-Recipe mapping
        recipes: List[NormalizedRecipe] = []
        machine_recipe_set: Set[Tuple[str, str]] = set()

        for r in payload.get("recipes", []):
            rid = str(r["id"]).strip()
            name = str(r.get("name", rid.replace("-", " ").capitalize())).strip()
            rcat_id = r.get("category")
            # Crafting time defaults to 0.5s if omitted
            time_val = float(r.get("time", 0.5))
            is_catalyst = bool(r.get("catalyst"))
            icon = r.get("icon", rid)

            # Ingest ingredients from "in" dict
            ingredients: List[NormalizedIngredient] = []
            in_dict = r.get("in", {})
            if isinstance(in_dict, dict):
                for ing_id, amt in in_dict.items():
                    ing_str = str(ing_id).strip()
                    # Resolve missing item reference if necessary
                    if ing_str not in item_ids:
                        items.append(NormalizedItem(
                            id=ing_str,
                            name=ing_str.replace("-", " ").capitalize(),
                            category_id="unknown",
                            stack_size=1,
                            icon=ing_str,
                            type="item"
                        ))
                        item_ids.add(ing_str)
                    ingredients.append(NormalizedIngredient(item_id=ing_str, amount=float(amt)))

            # Ingest products from "out" dict
            products: List[NormalizedProduct] = []
            out_dict = r.get("out", {})
            if isinstance(out_dict, dict):
                for prod_id, amt in out_dict.items():
                    prod_str = str(prod_id).strip()
                    # Resolve missing item reference if necessary
                    if prod_str not in item_ids:
                        items.append(NormalizedItem(
                            id=prod_str,
                            name=prod_str.replace("-", " ").capitalize(),
                            category_id="unknown",
                            stack_size=1,
                            icon=prod_str,
                            type="item"
                        ))
                        item_ids.add(prod_str)
                    
                    amount_val = float(amt)
                    # Preserve exact fractional outputs (like recycling 0.25 or uranium 0.007)
                    probability = 1.0
                    products.append(NormalizedProduct(item_id=prod_str, amount=amount_val, probability=probability))

            # Machine producers list
            producers = [str(p).strip() for p in r.get("producers", [])]
            for p in producers:
                if rcat_id:
                    machine_recipe_set.add((p, rcat_id))

            recipes.append(NormalizedRecipe(
                id=rid,
                name=name,
                category_id=rcat_id,
                crafting_time=time_val,
                is_catalyst=is_catalyst,
                icon=icon,
                ingredients=ingredients,
                products=products,
                producers=producers
            ))

        machine_recipes = [
            NormalizedMachineRecipe(machine_id=m_id, recipe_category=cat)
            for m_id, cat in sorted(machine_recipe_set)
        ]

        # Metadata extraction
        raw_version = payload.get("version", {})
        if isinstance(raw_version, dict):
            version_str = ", ".join(f"{k}:{v}" for k, v in raw_version.items())
        else:
            version_str = str(raw_version)

        metadata = {
            "source_version": version_str,
            "dataset": metadata_overrides.get("dataset", "unknown"),
            "source_url": metadata_overrides.get("source_url", "https://github.com/factoriolab/factoriolab"),
            "fetch_timestamp": metadata_overrides.get("fetch_timestamp", datetime.now(timezone.utc).isoformat()),
            "schema_version": "1.0.0",
            "importer_version": "1.0.0",
            "content_checksum": metadata_overrides.get("content_checksum", ""),
            "categories_count": str(len(categories)),
            "items_count": str(len(items)),
            "machines_count": str(len(machines)),
            "recipes_count": str(len(recipes)),
        }

        return ParsedDataset(
            categories=categories,
            items=items,
            machines=machines,
            recipes=recipes,
            machine_recipes=machine_recipes,
            metadata=metadata
        )


class FactorioPrototypeAdapter(SourceAdapter):
    """
    Fallback adapter for raw Factorio prototype dumps (e.g. wube/factorio-data, community JSON dumps).
    Handles:
    - Normal vs expensive difficulty variants policy.
    - Singular `result` / `result_count` vs plural `results` array.
    - Energy string parsing (e.g., '150kW', '3.5MW') to kW.
    - Reference resolution and duplicate ID protection.
    """

    def __init__(self, difficulty: str = "normal"):
        self.difficulty = difficulty.lower()

    def fetch(self, dataset_or_path: str, custom_url: Optional[str] = None) -> Tuple[Dict[str, Any], str, str]:
        if custom_url:
            url = custom_url
        elif dataset_or_path.startswith("http://") or dataset_or_path.startswith("https://"):
            url = dataset_or_path
        else:
            # Local file path
            with open(dataset_or_path, "rb") as f:
                content_bytes = f.read()
            checksum = compute_sha256(content_bytes)
            return json.loads(content_bytes.decode("utf-8")), f"file://{dataset_or_path}", checksum

        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "Factoria-DataPipeline/1.0",
                "Accept": "application/json"
            }
        )
        with urllib.request.urlopen(req, timeout=30) as response:
            if response.status != 200:
                raise RuntimeError(f"HTTP {response.status} when fetching {url}")
            content_bytes = response.read()

        checksum = compute_sha256(content_bytes)
        payload = json.loads(content_bytes.decode("utf-8"))
        return payload, url, checksum

    def parse(self, payload: Dict[str, Any], metadata_overrides: Optional[Dict[str, str]] = None) -> ParsedDataset:
        metadata_overrides = metadata_overrides or {}

        # 1. Categories
        categories: List[NormalizedCategory] = []
        category_ids: Set[str] = set()

        raw_item_groups = payload.get("item-group", {})
        if isinstance(raw_item_groups, dict):
            for gid, gdata in raw_item_groups.items():
                name = gdata.get("name", gid) if isinstance(gdata, dict) else gid
                categories.append(NormalizedCategory(id=gid, name=name, icon=gid))
                category_ids.add(gid)
        elif isinstance(raw_item_groups, list):
            for g in raw_item_groups:
                gid = g["name"]
                categories.append(NormalizedCategory(id=gid, name=g.get("name", gid), icon=gid))
                category_ids.add(gid)

        if "unknown" not in category_ids:
            categories.append(NormalizedCategory(id="unknown", name="Uncategorized", icon=None))
            category_ids.add("unknown")

        # 2. Items & Fluids
        items: List[NormalizedItem] = []
        item_ids: Set[str] = set()

        # Ingest items
        raw_items = payload.get("item", {})
        if isinstance(raw_items, dict):
            for iid, idata in raw_items.items():
                cat = idata.get("group", "unknown") if isinstance(idata, dict) else "unknown"
                stack = int(idata.get("stack_size", 50)) if isinstance(idata, dict) else 50
                items.append(NormalizedItem(id=iid, name=iid.replace("-", " ").capitalize(), category_id=cat, stack_size=stack, icon=iid, type="item"))
                item_ids.add(iid)

        # Ingest fluids
        raw_fluids = payload.get("fluid", {})
        if isinstance(raw_fluids, dict):
            for fid, fdata in raw_fluids.items():
                items.append(NormalizedItem(id=fid, name=fid.replace("-", " ").capitalize(), category_id="fluids" if "fluids" in category_ids else "unknown", stack_size=1, icon=fid, type="fluid"))
                item_ids.add(fid)

        # 3. Machines / Assembling machines / Furnaces
        machines: List[NormalizedMachine] = []
        machine_recipe_set: Set[Tuple[str, str]] = set()

        machine_sections = ["assembling-machine", "furnace", "rocket-silo", "mining-drill", "boiler", "reactor"]
        for section in machine_sections:
            sec_data = payload.get(section, {})
            if isinstance(sec_data, dict):
                for mid, mdata in sec_data.items():
                    speed = float(mdata.get("crafting_speed", 1.0))
                    energy_kw = parse_energy_to_kw(mdata.get("energy_usage", 0.0))
                    pollution = float(mdata.get("emissions_per_second_per_watt", 0.0)) * 60.0
                    mod_spec = mdata.get("module_specification")
                    if isinstance(mod_spec, dict) and "module_slots" in mod_spec:
                        slots = int(mod_spec.get("module_slots", 0))
                    else:
                        slots = int(mdata.get("module_slots", 0))
                    
                    machines.append(NormalizedMachine(
                        id=mid,
                        name=mid.replace("-", " ").capitalize(),
                        category="production",
                        crafting_speed=speed,
                        energy_usage_kw=energy_kw,
                        pollution_per_minute=pollution,
                        module_slots=slots,
                        icon=mid
                    ))
                    # Record crafting categories
                    craft_cats = mdata.get("crafting_categories", ["crafting"])
                    for cc in craft_cats:
                        machine_recipe_set.add((mid, str(cc)))

        # 4. Recipes
        recipes: List[NormalizedRecipe] = []
        raw_recipes = payload.get("recipe", {})
        if isinstance(raw_recipes, dict):
            for rid, rdata in raw_recipes.items():
                if not isinstance(rdata, dict):
                    continue

                # Policy: Normal vs Expensive variant selection
                variant = rdata
                if self.difficulty in rdata and isinstance(rdata[self.difficulty], dict):
                    variant = rdata[self.difficulty]
                elif "normal" in rdata and isinstance(rdata["normal"], dict):
                    variant = rdata["normal"]

                crafting_time = float(variant.get("energy_required", rdata.get("energy_required", 0.5)))
                cat_id = rdata.get("category", "crafting")

                # Parse ingredients
                ingredients: List[NormalizedIngredient] = []
                raw_ings = variant.get("ingredients", rdata.get("ingredients", []))
                for ing in raw_ings:
                    if isinstance(ing, dict):
                        ing_id = ing.get("name")
                        amount = float(ing.get("amount", 1.0))
                    elif isinstance(ing, (list, tuple)) and len(ing) >= 2:
                        ing_id = str(ing[0])
                        amount = float(ing[1])
                    else:
                        continue

                    if ing_id:
                        if ing_id not in item_ids:
                            items.append(NormalizedItem(id=ing_id, name=ing_id.replace("-", " ").capitalize(), category_id="unknown", stack_size=1, icon=ing_id, type="item"))
                            item_ids.add(ing_id)
                        ingredients.append(NormalizedIngredient(item_id=ing_id, amount=amount))

                # Parse products: singular result vs plural results
                products: List[NormalizedProduct] = []
                if "results" in variant and isinstance(variant["results"], list):
                    for prod in variant["results"]:
                        if isinstance(prod, dict):
                            prod_id = prod.get("name")
                            amount = float(prod.get("amount", prod.get("amount_max", 1.0)))
                            prob = float(prod.get("probability", 1.0))
                            if prod_id:
                                if prod_id not in item_ids:
                                    items.append(NormalizedItem(id=prod_id, name=prod_id.replace("-", " ").capitalize(), category_id="unknown", stack_size=1, icon=prod_id, type="item"))
                                    item_ids.add(prod_id)
                                products.append(NormalizedProduct(item_id=prod_id, amount=amount, probability=prob))
                elif "result" in variant:
                    prod_id = variant["result"]
                    amount = float(variant.get("result_count", 1.0))
                    if prod_id not in item_ids:
                        items.append(NormalizedItem(id=prod_id, name=prod_id.replace("-", " ").capitalize(), category_id="unknown", stack_size=1, icon=prod_id, type="item"))
                        item_ids.add(prod_id)
                    products.append(NormalizedProduct(item_id=prod_id, amount=amount, probability=1.0))

                recipes.append(NormalizedRecipe(
                    id=rid,
                    name=rid.replace("-", " ").capitalize(),
                    category_id=cat_id,
                    crafting_time=crafting_time,
                    is_catalyst=False,
                    icon=rid,
                    ingredients=ingredients,
                    products=products
                ))

        machine_recipes = [
            NormalizedMachineRecipe(machine_id=m_id, recipe_category=cat)
            for m_id, cat in sorted(machine_recipe_set)
        ]

        metadata = {
            "source_version": str(payload.get("version", "1.0.0")),
            "dataset": metadata_overrides.get("dataset", "prototype-dump"),
            "source_url": metadata_overrides.get("source_url", "local/wube"),
            "fetch_timestamp": metadata_overrides.get("fetch_timestamp", datetime.now(timezone.utc).isoformat()),
            "schema_version": "1.0.0",
            "importer_version": "1.0.0",
            "content_checksum": metadata_overrides.get("content_checksum", ""),
            "categories_count": str(len(categories)),
            "items_count": str(len(items)),
            "machines_count": str(len(machines)),
            "recipes_count": str(len(recipes)),
        }

        return ParsedDataset(
            categories=categories,
            items=items,
            machines=machines,
            recipes=recipes,
            machine_recipes=machine_recipes,
            metadata=metadata
        )
