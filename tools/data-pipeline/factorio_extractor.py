#!/usr/bin/env python3
"""
Factoria Build-Time Factorio Data Extraction Pipeline
Compatible with Factorio 2.0+ and the Space Age expansion.

Parses prototype dumps (from `factorio --dump-data`), normalizes domain models,
and compiles a compact, fully indexed SQLite database + manifest for offline use.
"""

import os
import sys
import json
import sqlite3
import argparse
import hashlib
import datetime
from typing import Dict, Any, List, Optional, Tuple

VERSION = "2.0.0"
SCHEMA_VERSION = 2

def parse_energy(energy_str: Any) -> float:
    """Parses Factorio energy strings like '150kW', '3.5MW', '10MJ' into watts / joules."""
    if not energy_str or not isinstance(energy_str, str):
        if isinstance(energy_str, (int, float)):
            return float(energy_str)
        return 0.0
    s = energy_str.strip().lower()
    mult = 1.0
    if s.endswith("kw") or s.endswith("kj"):
        mult = 1_000.0
        s = s[:-2]
    elif s.endswith("mw") or s.endswith("mj"):
        mult = 1_000_000.0
        s = s[:-2]
    elif s.endswith("gw") or s.endswith("gj"):
        mult = 1_000_000_000.0
        s = s[:-2]
    elif s.endswith("w") or s.endswith("j"):
        mult = 1.0
        s = s[:-1]
    try:
        return float(s.strip()) * mult
    except ValueError:
        return 0.0

def normalize_ingredient(ing: Any) -> Optional[Dict[str, Any]]:
    """Normalizes shorthand and verbose Factorio ingredient definitions."""
    if isinstance(ing, (list, tuple)) and len(ing) >= 2:
        return {
            "type": "item",
            "name": str(ing[0]),
            "amount": float(ing[1]),
            "temperature": None,
            "minimum_temperature": None,
            "maximum_temperature": None
        }
    elif isinstance(ing, dict):
        itype = ing.get("type", "item")
        name = ing.get("name")
        amount = ing.get("amount", 1.0)
        return {
            "type": itype,
            "name": name,
            "amount": float(amount),
            "temperature": ing.get("temperature"),
            "minimum_temperature": ing.get("minimum_temperature"),
            "maximum_temperature": ing.get("maximum_temperature")
        }
    return None

def normalize_product(prod: Any) -> Optional[Dict[str, Any]]:
    """Normalizes shorthand and verbose Factorio product definitions with probabilities and catalysts."""
    if isinstance(prod, (list, tuple)) and len(prod) >= 2:
        return {
            "type": "item",
            "name": str(prod[0]),
            "amount": float(prod[1]),
            "amount_min": float(prod[1]),
            "amount_max": float(prod[1]),
            "probability": 1.0,
            "catalyst_amount": 0.0,
            "temperature": None
        }
    elif isinstance(prod, dict):
        ptype = prod.get("type", "item")
        name = prod.get("name")
        amount = prod.get("amount")
        amount_min = prod.get("amount_min")
        amount_max = prod.get("amount_max")
        probability = float(prod.get("probability", 1.0))
        catalyst = float(prod.get("catalyst_amount", 0.0))
        temp = prod.get("temperature")
        
        if amount is not None:
            a_min = float(amount)
            a_max = float(amount)
            a_val = float(amount)
        else:
            a_min = float(amount_min if amount_min is not None else 1.0)
            a_max = float(amount_max if amount_max is not None else a_min)
            a_val = (a_min + a_max) / 2.0
            
        return {
            "type": ptype,
            "name": name,
            "amount": a_val,
            "amount_min": a_min,
            "amount_max": a_max,
            "probability": probability,
            "catalyst_amount": catalyst,
            "temperature": temp
        }
    return None

def clean_localized_name(name: str) -> str:
    """Formats internal kebab-case names into title case (e.g. 'electronic-circuit' -> 'Electronic Circuit')."""
    return " ".join(word.capitalize() for word in name.replace("-", " ").replace("_", " ").split())

def init_database(db_path: str):
    """Initializes the SQLite schema with tables, indices, and FTS5 search index."""
    if os.path.exists(db_path):
        os.remove(db_path)
    
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    
    cur.executescript("""
    PRAGMA foreign_keys = ON;
    PRAGMA journal_mode = WAL;
    
    CREATE TABLE manifest (
        id INTEGER PRIMARY KEY,
        factorio_version TEXT NOT NULL,
        space_age_enabled INTEGER NOT NULL,
        schema_version INTEGER NOT NULL,
        generated_at TEXT NOT NULL,
        data_hash TEXT NOT NULL
    );
    
    CREATE TABLE items (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        localized_name TEXT NOT NULL,
        item_group TEXT NOT NULL,
        subgroup TEXT NOT NULL,
        stack_size INTEGER NOT NULL,
        fuel_value REAL DEFAULT 0.0,
        fuel_category TEXT,
        fuel_emissions REAL DEFAULT 1.0,
        spoilage_time REAL DEFAULT 0.0,
        icon_path TEXT
    );
    
    CREATE TABLE fluids (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        localized_name TEXT NOT NULL,
        default_temp REAL NOT NULL,
        max_temp REAL,
        heat_capacity REAL NOT NULL,
        fuel_value REAL NOT NULL DEFAULT 0.0,
        emissions_multiplier REAL NOT NULL DEFAULT 1.0,
        icon_path TEXT
    );
    
    CREATE TABLE recipes (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        localized_name TEXT NOT NULL,
        category TEXT NOT NULL,
        crafting_time REAL NOT NULL,
        is_craftable_in_hand INTEGER NOT NULL,
        is_catalyst_loop INTEGER NOT NULL DEFAULT 0,
        is_recycling INTEGER NOT NULL DEFAULT 0,
        unlocked_by_tech_id TEXT,
        subgroup TEXT
    );
    
    CREATE TABLE recipe_ingredients (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        recipe_id TEXT NOT NULL,
        type TEXT NOT NULL CHECK(type IN ('item', 'fluid')),
        target_id TEXT NOT NULL,
        amount REAL NOT NULL,
        temperature REAL,
        minimum_temperature REAL,
        maximum_temperature REAL,
        FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
    );
    
    CREATE TABLE recipe_products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        recipe_id TEXT NOT NULL,
        type TEXT NOT NULL CHECK(type IN ('item', 'fluid')),
        target_id TEXT NOT NULL,
        amount REAL,
        amount_min REAL,
        amount_max REAL,
        probability REAL NOT NULL DEFAULT 1.0,
        catalyst_amount REAL NOT NULL DEFAULT 0.0,
        temperature REAL,
        FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
    );
    
    CREATE TABLE entities (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        localized_name TEXT NOT NULL,
        type TEXT NOT NULL,
        crafting_speed REAL NOT NULL DEFAULT 1.0,
        mining_speed REAL NOT NULL DEFAULT 1.0,
        energy_usage REAL NOT NULL DEFAULT 0.0,
        drain_energy REAL NOT NULL DEFAULT 0.0,
        emissions_per_minute REAL NOT NULL DEFAULT 0.0,
        module_slots INTEGER NOT NULL DEFAULT 0,
        pollution_multiplier REAL NOT NULL DEFAULT 1.0,
        health REAL NOT NULL DEFAULT 100.0,
        icon_path TEXT
    );
    
    CREATE TABLE entity_crafting_categories (
        entity_id TEXT NOT NULL,
        category TEXT NOT NULL,
        PRIMARY KEY(entity_id, category),
        FOREIGN KEY(entity_id) REFERENCES entities(id) ON DELETE CASCADE
    );
    
    CREATE TABLE modules (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        tier INTEGER NOT NULL,
        category TEXT NOT NULL,
        speed_bonus REAL NOT NULL DEFAULT 0.0,
        productivity_bonus REAL NOT NULL DEFAULT 0.0,
        energy_bonus REAL NOT NULL DEFAULT 0.0,
        pollution_bonus REAL NOT NULL DEFAULT 0.0,
        quality_bonus REAL NOT NULL DEFAULT 0.0
    );
    
    CREATE TABLE module_limitations (
        module_id TEXT NOT NULL,
        recipe_id TEXT NOT NULL,
        PRIMARY KEY(module_id, recipe_id),
        FOREIGN KEY(module_id) REFERENCES modules(id) ON DELETE CASCADE
    );
    
    CREATE TABLE qualities (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        tier INTEGER NOT NULL,
        speed_modifier REAL NOT NULL,
        health_modifier REAL NOT NULL,
        module_effect_modifier REAL NOT NULL
    );
    
    CREATE TABLE technologies (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        localized_name TEXT NOT NULL,
        unit_count INTEGER NOT NULL,
        unit_time REAL NOT NULL,
        level_formula TEXT,
        max_level INTEGER,
        icon_path TEXT
    );
    
    CREATE TABLE technology_ingredients (
        technology_id TEXT NOT NULL,
        science_pack_id TEXT NOT NULL,
        amount INTEGER NOT NULL DEFAULT 1,
        PRIMARY KEY(technology_id, science_pack_id),
        FOREIGN KEY(technology_id) REFERENCES technologies(id) ON DELETE CASCADE
    );
    
    CREATE TABLE technology_prerequisites (
        technology_id TEXT NOT NULL,
        prerequisite_id TEXT NOT NULL,
        PRIMARY KEY(technology_id, prerequisite_id),
        FOREIGN KEY(technology_id) REFERENCES technologies(id) ON DELETE CASCADE
    );
    
    CREATE TABLE technology_unlocks (
        technology_id TEXT NOT NULL,
        recipe_id TEXT NOT NULL,
        PRIMARY KEY(technology_id, recipe_id),
        FOREIGN KEY(technology_id) REFERENCES technologies(id) ON DELETE CASCADE
    );
    
    CREATE INDEX idx_recipe_ing_target ON recipe_ingredients(target_id);
    CREATE INDEX idx_recipe_prod_target ON recipe_products(target_id);
    CREATE INDEX idx_recipe_ing_recipe ON recipe_ingredients(recipe_id);
    CREATE INDEX idx_recipe_prod_recipe ON recipe_products(recipe_id);
    CREATE INDEX idx_entity_cat_category ON entity_crafting_categories(category);
    CREATE INDEX idx_tech_prereq_tech ON technology_prerequisites(technology_id);
    CREATE INDEX idx_tech_prereq_pre ON technology_prerequisites(prerequisite_id);
    CREATE INDEX idx_tech_unlock_recipe ON technology_unlocks(recipe_id);
    
    CREATE VIRTUAL TABLE search_index USING fts4(
        id,
        type,
        name,
        localized_name,
        category,
        tokenize=porter
    );
    """)
    
    conn.commit()
    return conn

def populate_database(conn: sqlite3.Connection, data: Dict[str, Any], factorio_version: str = "2.0.32", is_space_age: bool = True):
    cur = conn.cursor()
    
    # 1. Qualities
    qualities_raw = data.get("quality", {})
    if not qualities_raw:
        # Default Factorio 2.0 qualities
        qualities_raw = {
            "normal": {"level": 0, "name": "normal"},
            "uncommon": {"level": 1, "name": "uncommon"},
            "rare": {"level": 2, "name": "rare"},
            "epic": {"level": 3, "name": "epic"},
            "legendary": {"level": 4, "name": "legendary"}
        }
    for q_name, q in qualities_raw.items():
        tier = q.get("level", 0)
        speed_mod = 1.0 + (tier * 0.3)
        health_mod = 1.0 + (tier * 0.3)
        module_mod = 1.0 + (tier * 0.3)
        cur.execute("INSERT OR REPLACE INTO qualities VALUES (?, ?, ?, ?, ?, ?)",
                    (q_name, clean_localized_name(q_name), tier, speed_mod, health_mod, module_mod))
        cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                    (q_name, "quality", q_name, clean_localized_name(q_name), "Quality"))
    
    # 2. Items
    item_categories = ["item", "ammo", "capsule", "gun", "armor", "blueprint", "rail-planner", "tool", "repair-tool", "spidertron-remote", "module"]
    seen_items = set()
    for cat in item_categories:
        for item_name, item in data.get(cat, {}).items():
            if item_name in seen_items:
                continue
            seen_items.add(item_name)
            loc_name = clean_localized_name(item_name)
            subgroup = item.get("subgroup", "other")
            group = item.get("group", "intermediate-products")
            stack_size = int(item.get("stack_size", 50))
            fuel_val = parse_energy(item.get("fuel_value"))
            fuel_cat = item.get("fuel_category")
            fuel_emiss = float(item.get("fuel_emissions_multiplier", 1.0))
            spoil_ticks = item.get("spoil_ticks", 0)
            spoil_sec = spoil_ticks / 60.0 if spoil_ticks else 0.0
            icon = item.get("icon", "")
            
            cur.execute("INSERT OR REPLACE INTO items VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        (item_name, item_name, loc_name, group, subgroup, stack_size, fuel_val, fuel_cat, fuel_emiss, spoil_sec, icon))
            cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                        (item_name, "item", item_name, loc_name, group))

    # 3. Fluids
    for fluid_name, fluid in data.get("fluid", {}).items():
        loc_name = clean_localized_name(fluid_name)
        def_temp = float(fluid.get("default_temperature", 15.0))
        max_temp = float(fluid.get("max_temperature", 100.0)) if "max_temperature" in fluid else None
        heat_cap = parse_energy(fluid.get("heat_capacity", "1kJ"))
        fuel_val = parse_energy(fluid.get("fuel_value"))
        emiss = float(fluid.get("emissions_multiplier", 1.0))
        icon = fluid.get("icon", "")
        
        cur.execute("INSERT OR REPLACE INTO fluids VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (fluid_name, fluid_name, loc_name, def_temp, max_temp, heat_cap, fuel_val, emiss, icon))
        cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                    (fluid_name, "fluid", fluid_name, loc_name, "Fluid"))

    # 4. Modules
    for mod_name, mod in data.get("module", {}).items():
        tier = int(mod.get("tier", 1))
        category = mod.get("category", "speed")
        effect = mod.get("effect", {})
        speed_b = float(effect.get("speed", {}).get("bonus", 0.0))
        prod_b = float(effect.get("productivity", {}).get("bonus", 0.0))
        energy_b = float(effect.get("consumption", {}).get("bonus", 0.0))
        poll_b = float(effect.get("pollution", {}).get("bonus", 0.0))
        qual_b = float(effect.get("quality", {}).get("bonus", 0.0))
        
        cur.execute("INSERT OR REPLACE INTO modules VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (mod_name, mod_name, tier, category, speed_b, prod_b, energy_b, poll_b, qual_b))
        
        # Module limitations (e.g. productivity allowed recipes)
        limitation = mod.get("limitation", [])
        for recipe_id in limitation:
            cur.execute("INSERT OR IGNORE INTO module_limitations VALUES (?, ?)", (mod_name, recipe_id))

    # 5. Recipes
    for rec_name, rec in data.get("recipe", {}).items():
        loc_name = clean_localized_name(rec_name)
        category = rec.get("category", "crafting")
        crafting_time = float(rec.get("energy_required", rec.get("energy", 0.5)))
        is_hand = 1 if rec.get("category", "crafting") == "crafting" else 0
        subgroup = rec.get("subgroup", "other")
        
        # Ingredients
        raw_ings = rec.get("ingredients", [])
        if not raw_ings and "normal" in rec:
            raw_ings = rec["normal"].get("ingredients", [])
        
        # Products / Results
        raw_prods = []
        if "results" in rec:
            raw_prods = rec["results"]
        elif "result" in rec:
            raw_prods = [{"name": rec["result"], "amount": rec.get("result_count", 1), "type": "item"}]
        elif "normal" in rec:
            if "results" in rec["normal"]:
                raw_prods = rec["normal"]["results"]
            elif "result" in rec["normal"]:
                raw_prods = [{"name": rec["normal"]["result"], "amount": rec["normal"].get("result_count", 1), "type": "item"}]
        
        normalized_ings = [normalize_ingredient(i) for i in raw_ings if normalize_ingredient(i)]
        normalized_prods = [normalize_product(p) for p in raw_prods if normalize_product(p)]
        
        # Detect catalyst cycle (e.g. Kovarex)
        ing_targets = {i["name"] for i in normalized_ings}
        prod_targets = {p["name"] for p in normalized_prods}
        is_catalyst = 1 if (ing_targets & prod_targets) else 0
        is_recycling = 1 if "recycling" in category or "recycler" in rec_name else 0
        
        cur.execute("INSERT OR REPLACE INTO recipes VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?)",
                    (rec_name, rec_name, loc_name, category, crafting_time, is_hand, is_catalyst, is_recycling, subgroup))
        cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                    (rec_name, "recipe", rec_name, loc_name, category))
        
        for ing in normalized_ings:
            cur.execute("""
            INSERT INTO recipe_ingredients (recipe_id, type, target_id, amount, temperature, minimum_temperature, maximum_temperature)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (rec_name, ing["type"], ing["name"], ing["amount"], ing["temperature"], ing["minimum_temperature"], ing["maximum_temperature"]))
            
        for prod in normalized_prods:
            cur.execute("""
            INSERT INTO recipe_products (recipe_id, type, target_id, amount, amount_min, amount_max, probability, catalyst_amount, temperature)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (rec_name, prod["type"], prod["name"], prod["amount"], prod["amount_min"], prod["amount_max"], prod["probability"], prod["catalyst_amount"], prod["temperature"]))

    # 6. Entities (Machines, Drills, Beacons, Furnaces, Generators, Belts, Inserters, etc.)
    entity_proto_types = [
        "assembling-machine", "furnace", "rocket-silo", "mining-drill", "lab", "beacon",
        "boiler", "generator", "reactor", "transport-belt", "underground-belt", "splitter",
        "inserter", "pipe", "pump", "storage-tank", "locomotive", "cargo-wagon", "fluid-wagon"
    ]
    seen_entities = set()
    for proto_type in entity_proto_types:
        for ent_name, ent in data.get(proto_type, {}).items():
            if ent_name in seen_entities:
                continue
            seen_entities.add(ent_name)
            loc_name = clean_localized_name(ent_name)
            craft_speed = float(ent.get("crafting_speed", ent.get("mining_speed", 1.0)))
            mining_speed = float(ent.get("mining_speed", 1.0))
            energy_usage = parse_energy(ent.get("energy_usage", ent.get("max_energy_usage", "0W")))
            drain = parse_energy(ent.get("drain", "0W"))
            mod_slots = int(ent.get("module_slots", ent.get("module_specification", {}).get("module_slots", 0)))
            health = float(ent.get("max_health", 100.0))
            icon = ent.get("icon", "")
            emiss = float(ent.get("energy_source", {}).get("emissions_per_minute", 0.0))
            
            cur.execute("""
            INSERT OR REPLACE INTO entities VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (ent_name, ent_name, loc_name, proto_type, craft_speed, mining_speed, energy_usage, drain, emiss, mod_slots, 1.0, health, icon))
            cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                        (ent_name, "entity", ent_name, loc_name, proto_type))
            
            # Crafting categories
            craft_cats = ent.get("crafting_categories", [])
            for ccat in craft_cats:
                cur.execute("INSERT OR REPLACE INTO entity_crafting_categories VALUES (?, ?)", (ent_name, ccat))

    # 7. Technologies
    for tech_name, tech in data.get("technology", {}).items():
        loc_name = clean_localized_name(tech_name)
        unit = tech.get("unit", {})
        unit_count = int(unit.get("count", 100)) if "count" in unit else 100
        unit_time = float(unit.get("time", 30.0))
        formula = unit.get("count_formula")
        max_level = int(tech.get("max_level", 1)) if "max_level" in tech else (None if formula else 1)
        icon = tech.get("icon", "")
        
        cur.execute("INSERT OR REPLACE INTO technologies VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (tech_name, tech_name, loc_name, unit_count, unit_time, formula, max_level, icon))
        cur.execute("INSERT INTO search_index VALUES (?, ?, ?, ?, ?)",
                    (tech_name, "technology", tech_name, loc_name, "Technology"))
        
        # Science packs
        for sp in unit.get("ingredients", []):
            if isinstance(sp, (list, tuple)) and len(sp) >= 2:
                cur.execute("INSERT OR REPLACE INTO technology_ingredients VALUES (?, ?, ?)", (tech_name, sp[0], sp[1]))
            elif isinstance(sp, dict):
                cur.execute("INSERT OR REPLACE INTO technology_ingredients VALUES (?, ?, ?)", (tech_name, sp["name"], sp.get("amount", 1)))
                
        # Prerequisites
        for prereq in tech.get("prerequisites", []):
            cur.execute("INSERT OR REPLACE INTO technology_prerequisites VALUES (?, ?)", (tech_name, prereq))
            
        # Effects (unlock recipes)
        for effect in tech.get("effects", []):
            if effect.get("type") == "unlock-recipe":
                rec_id = effect.get("recipe")
                if rec_id:
                    cur.execute("INSERT OR REPLACE INTO technology_unlocks VALUES (?, ?)", (tech_name, rec_id))
                    cur.execute("UPDATE recipes SET unlocked_by_tech_id = ? WHERE id = ?", (tech_name, rec_id))

    # Manifest record
    data_hash = hashlib.sha256(json.dumps(data, sort_keys=True).encode("utf-8")).hexdigest()
    now_str = datetime.datetime.now(datetime.timezone.utc).isoformat().replace("+00:00", "Z")
    cur.execute("INSERT INTO manifest VALUES (1, ?, ?, ?, ?, ?)",
                (factorio_version, 1 if is_space_age else 0, SCHEMA_VERSION, now_str, data_hash))
    
    conn.commit()

def generate_sample_factorio_dataset() -> Dict[str, Any]:
    """
    Generates a realistic, highly detailed Factorio 2.0 + Space Age prototype dataset
    including base game and Space Age recipes, machines, quality, Kovarex, oil cracking,
    foundries, electromagnetic plants, biochambers, and recyclers.
    """
    return {
        "quality": {
            "normal": {"level": 0, "name": "normal"},
            "uncommon": {"level": 1, "name": "uncommon"},
            "rare": {"level": 2, "name": "rare"},
            "epic": {"level": 3, "name": "epic"},
            "legendary": {"level": 4, "name": "legendary"}
        },
        "item": {
            "iron-ore": {"stack_size": 50, "group": "raw-resource", "subgroup": "ore"},
            "copper-ore": {"stack_size": 50, "group": "raw-resource", "subgroup": "ore"},
            "coal": {"stack_size": 50, "group": "raw-resource", "subgroup": "ore", "fuel_value": "4MJ", "fuel_category": "chemical"},
            "stone": {"stack_size": 100, "group": "raw-resource", "subgroup": "ore"},
            "uranium-ore": {"stack_size": 50, "group": "raw-resource", "subgroup": "ore"},
            "iron-plate": {"stack_size": 100, "group": "intermediate-products", "subgroup": "plates"},
            "copper-plate": {"stack_size": 100, "group": "intermediate-products", "subgroup": "plates"},
            "steel-plate": {"stack_size": 100, "group": "intermediate-products", "subgroup": "plates"},
            "iron-gear-wheel": {"stack_size": 100, "group": "intermediate-products", "subgroup": "parts"},
            "electronic-circuit": {"stack_size": 200, "group": "intermediate-products", "subgroup": "electronics"},
            "advanced-circuit": {"stack_size": 200, "group": "intermediate-products", "subgroup": "electronics"},
            "processing-unit": {"stack_size": 100, "group": "intermediate-products", "subgroup": "electronics"},
            "plastic-bar": {"stack_size": 100, "group": "intermediate-products", "subgroup": "chemicals"},
            "sulfur": {"stack_size": 100, "group": "intermediate-products", "subgroup": "chemicals"},
            "uranium-235": {"stack_size": 100, "group": "intermediate-products", "subgroup": "nuclear"},
            "uranium-238": {"stack_size": 100, "group": "intermediate-products", "subgroup": "nuclear"},
            "uranium-fuel-cell": {"stack_size": 50, "group": "intermediate-products", "subgroup": "nuclear", "fuel_value": "8GJ", "fuel_category": "nuclear"},
            "used-up-uranium-fuel-cell": {"stack_size": 50, "group": "intermediate-products", "subgroup": "nuclear"},
            # Space Age Items
            "calcite": {"stack_size": 50, "group": "space-age", "subgroup": "vulcanus"},
            "tungsten-ore": {"stack_size": 50, "group": "space-age", "subgroup": "vulcanus"},
            "tungsten-carbide": {"stack_size": 100, "group": "space-age", "subgroup": "vulcanus"},
            "tungsten-plate": {"stack_size": 100, "group": "space-age", "subgroup": "vulcanus"},
            "holmium-ore": {"stack_size": 50, "group": "space-age", "subgroup": "fulgora"},
            "holmium-plate": {"stack_size": 100, "group": "space-age", "subgroup": "fulgora"},
            "superconductor": {"stack_size": 100, "group": "space-age", "subgroup": "fulgora"},
            "supercapacitor": {"stack_size": 100, "group": "space-age", "subgroup": "fulgora"},
            "scrap": {"stack_size": 100, "group": "space-age", "subgroup": "fulgora"},
            "yumako": {"stack_size": 100, "group": "space-age", "subgroup": "gleba", "spoil_ticks": 18000},
            "jellynut": {"stack_size": 100, "group": "space-age", "subgroup": "gleba", "spoil_ticks": 18000},
            "bioflux": {"stack_size": 100, "group": "space-age", "subgroup": "gleba", "spoil_ticks": 36000},
            "spoilage": {"stack_size": 200, "group": "space-age", "subgroup": "gleba", "fuel_value": "1MJ", "fuel_category": "chemical"},
            # Science Packs
            "automation-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "logistic-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "chemical-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "military-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "production-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "utility-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "space-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "metallurgic-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "electromagnetic-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "agricultural-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science", "spoil_ticks": 18000},
            "cryogenic-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"},
            "promethium-science-pack": {"stack_size": 200, "group": "science-packs", "subgroup": "science"}
        },
        "fluid": {
            "water": {"default_temperature": 15, "max_temperature": 100, "heat_capacity": "1kJ"},
            "steam": {"default_temperature": 165, "max_temperature": 1000, "heat_capacity": "1kJ"},
            "crude-oil": {"default_temperature": 15, "heat_capacity": "1kJ"},
            "heavy-oil": {"default_temperature": 15, "heat_capacity": "1kJ"},
            "light-oil": {"default_temperature": 15, "heat_capacity": "1kJ", "fuel_value": "1.5MJ"},
            "petroleum-gas": {"default_temperature": 15, "heat_capacity": "1kJ", "fuel_value": "2.3MJ"},
            "sulfuric-acid": {"default_temperature": 15, "heat_capacity": "1kJ"},
            "lubricant": {"default_temperature": 15, "heat_capacity": "1kJ"},
            # Space Age Fluids
            "lava": {"default_temperature": 1500, "heat_capacity": "1kJ"},
            "molten-iron": {"default_temperature": 1500, "heat_capacity": "1kJ"},
            "molten-copper": {"default_temperature": 1500, "heat_capacity": "1kJ"},
            "ammoniacal-solution": {"default_temperature": 15, "heat_capacity": "1kJ"},
            "fluorine": {"default_temperature": 15, "heat_capacity": "1kJ"}
        },
        "recipe": {
            "iron-plate": {"category": "smelting", "energy_required": 3.2, "ingredients": [["iron-ore", 1]], "result": "iron-plate"},
            "copper-plate": {"category": "smelting", "energy_required": 3.2, "ingredients": [["copper-ore", 1]], "result": "copper-plate"},
            "steel-plate": {"category": "smelting", "energy_required": 16.0, "ingredients": [["iron-plate", 5]], "result": "steel-plate"},
            "iron-gear-wheel": {"category": "crafting", "energy_required": 0.5, "ingredients": [["iron-plate", 2]], "result": "iron-gear-wheel"},
            "electronic-circuit": {"category": "crafting", "energy_required": 0.5, "ingredients": [["iron-plate", 1], ["copper-plate", 3]], "result": "electronic-circuit"},
            "advanced-circuit": {"category": "crafting", "energy_required": 6.0, "ingredients": [["electronic-circuit", 2], ["plastic-bar", 2], ["copper-plate", 4]], "result": "advanced-circuit"},
            "processing-unit": {"category": "crafting", "energy_required": 10.0, "ingredients": [["electronic-circuit", 20], ["advanced-circuit", 2], {"name": "sulfuric-acid", "type": "fluid", "amount": 5}], "result": "processing-unit"},
            # Oil & Cracking Loops
            "basic-oil-processing": {
                "category": "oil-processing", "energy_required": 5.0,
                "ingredients": [{"name": "crude-oil", "type": "fluid", "amount": 100}],
                "results": [{"name": "petroleum-gas", "type": "fluid", "amount": 45}]
            },
            "advanced-oil-processing": {
                "category": "oil-processing", "energy_required": 5.0,
                "ingredients": [{"name": "crude-oil", "type": "fluid", "amount": 100}, {"name": "water", "type": "fluid", "amount": 50}],
                "results": [{"name": "heavy-oil", "type": "fluid", "amount": 25}, {"name": "light-oil", "type": "fluid", "amount": 45}, {"name": "petroleum-gas", "type": "fluid", "amount": 55}]
            },
            "heavy-oil-cracking": {
                "category": "chemistry", "energy_required": 2.0,
                "ingredients": [{"name": "heavy-oil", "type": "fluid", "amount": 40}, {"name": "water", "type": "fluid", "amount": 30}],
                "results": [{"name": "light-oil", "type": "fluid", "amount": 30}]
            },
            "light-oil-cracking": {
                "category": "chemistry", "energy_required": 2.0,
                "ingredients": [{"name": "light-oil", "type": "fluid", "amount": 30}, {"name": "water", "type": "fluid", "amount": 30}],
                "results": [{"name": "petroleum-gas", "type": "fluid", "amount": 20}]
            },
            # Nuclear & Kovarex Enrichment Cycle
            "uranium-processing": {
                "category": "centrifuging", "energy_required": 12.0,
                "ingredients": [["uranium-ore", 10]],
                "results": [
                    {"name": "uranium-235", "type": "item", "probability": 0.007, "amount": 1},
                    {"name": "uranium-238", "type": "item", "probability": 0.993, "amount": 1}
                ]
            },
            "kovarex-enrichment-process": {
                "category": "centrifuging", "energy_required": 60.0,
                "ingredients": [["uranium-235", 40], ["uranium-238", 5]],
                "results": [
                    {"name": "uranium-235", "type": "item", "amount": 41, "catalyst_amount": 40},
                    {"name": "uranium-238", "type": "item", "amount": 2, "catalyst_amount": 2}
                ]
            },
            # Space Age Foundries & Recycling
            "molten-iron-from-lava": {
                "category": "metallurgy", "energy_required": 3.2,
                "ingredients": [{"name": "lava", "type": "fluid", "amount": 500}, ["calcite", 1]],
                "results": [{"name": "molten-iron", "type": "fluid", "amount": 250}, {"name": "stone", "type": "item", "amount": 10}]
            },
            "casting-iron-plate": {
                "category": "casting", "energy_required": 3.2,
                "ingredients": [{"name": "molten-iron", "type": "fluid", "amount": 20}],
                "results": [{"name": "iron-plate", "type": "item", "amount": 3}]
            },
            "scrap-recycling": {
                "category": "recycling", "energy_required": 0.0625,
                "ingredients": [["scrap", 1]],
                "results": [
                    {"name": "iron-gear-wheel", "type": "item", "amount": 1, "probability": 0.20},
                    {"name": "copper-plate", "type": "item", "amount": 1, "probability": 0.20},
                    {"name": "electronic-circuit", "type": "item", "amount": 1, "probability": 0.10},
                    {"name": "steel-plate", "type": "item", "amount": 1, "probability": 0.05},
                    {"name": "processing-unit", "type": "item", "amount": 1, "probability": 0.01}
                ]
            }
        },
        "assembling-machine": {
            "assembling-machine-1": {"crafting_speed": 0.5, "energy_usage": "75kW", "drain": "2.5kW", "module_slots": 0, "max_health": 300, "crafting_categories": ["crafting"]},
            "assembling-machine-2": {"crafting_speed": 0.75, "energy_usage": "150kW", "drain": "5kW", "module_slots": 2, "max_health": 350, "crafting_categories": ["crafting", "advanced-crafting"]},
            "assembling-machine-3": {"crafting_speed": 1.25, "energy_usage": "375kW", "drain": "12.5kW", "module_slots": 4, "max_health": 400, "crafting_categories": ["crafting", "advanced-crafting"]},
            # Space Age Machines
            "foundry": {"crafting_speed": 4.0, "energy_usage": "2.5MW", "drain": "50kW", "module_slots": 4, "max_health": 800, "crafting_categories": ["smelting", "metallurgy", "casting"]},
            "electromagnetic-plant": {"crafting_speed": 2.0, "energy_usage": "2.0MW", "drain": "40kW", "module_slots": 5, "max_health": 600, "crafting_categories": ["crafting", "electromagnetics"]},
            "biochamber": {"crafting_speed": 2.0, "energy_usage": "500kW", "drain": "20kW", "module_slots": 4, "max_health": 500, "crafting_categories": ["organic", "organic-or-chemistry"]},
            "recycler": {"crafting_speed": 16.0, "energy_usage": "300kW", "drain": "10kW", "module_slots": 4, "max_health": 400, "crafting_categories": ["recycling"]}
        },
        "furnace": {
            "stone-furnace": {"crafting_speed": 1.0, "energy_usage": "90kW", "module_slots": 0, "max_health": 200, "crafting_categories": ["smelting"]},
            "steel-furnace": {"crafting_speed": 2.0, "energy_usage": "90kW", "module_slots": 0, "max_health": 300, "crafting_categories": ["smelting"]},
            "electric-furnace": {"crafting_speed": 2.0, "energy_usage": "180kW", "drain": "6kW", "module_slots": 2, "max_health": 350, "crafting_categories": ["smelting"]}
        },
        "mining-drill": {
            "burner-mining-drill": {"mining_speed": 0.25, "energy_usage": "150kW", "module_slots": 0, "max_health": 150},
            "electric-mining-drill": {"mining_speed": 0.5, "energy_usage": "90kW", "drain": "3kW", "module_slots": 3, "max_health": 300},
            "big-mining-drill": {"mining_speed": 2.5, "energy_usage": "1.5MW", "drain": "30kW", "module_slots": 4, "max_health": 1000}
        },
        "beacon": {
            "beacon": {"energy_usage": "480kW", "module_slots": 2, "max_health": 400}
        },
        "module": {
            "speed-module": {"tier": 1, "category": "speed", "effect": {"speed": {"bonus": 0.20}, "consumption": {"bonus": 0.50}}},
            "speed-module-2": {"tier": 2, "category": "speed", "effect": {"speed": {"bonus": 0.30}, "consumption": {"bonus": 0.60}}},
            "speed-module-3": {"tier": 3, "category": "speed", "effect": {"speed": {"bonus": 0.50}, "consumption": {"bonus": 0.70}}},
            "productivity-module": {"tier": 1, "category": "productivity", "effect": {"productivity": {"bonus": 0.04}, "speed": {"bonus": -0.05}, "consumption": {"bonus": 0.40}}},
            "productivity-module-2": {"tier": 2, "category": "productivity", "effect": {"productivity": {"bonus": 0.06}, "speed": {"bonus": -0.10}, "consumption": {"bonus": 0.60}}},
            "productivity-module-3": {"tier": 3, "category": "productivity", "effect": {"productivity": {"bonus": 0.10}, "speed": {"bonus": -0.15}, "consumption": {"bonus": 0.80}}},
            "quality-module": {"tier": 1, "category": "quality", "effect": {"quality": {"bonus": 0.01}, "speed": {"bonus": -0.05}}},
            "quality-module-2": {"tier": 2, "category": "quality", "effect": {"quality": {"bonus": 0.02}, "speed": {"bonus": -0.05}}},
            "quality-module-3": {"tier": 3, "category": "quality", "effect": {"quality": {"bonus": 0.025}, "speed": {"bonus": -0.05}}}
        },
        "technology": {
            "automation": {"unit": {"count": 10, "time": 10, "ingredients": [["automation-science-pack", 1]]}, "effects": [{"type": "unlock-recipe", "recipe": "iron-gear-wheel"}]},
            "oil-processing": {"unit": {"count": 100, "time": 30, "ingredients": [["automation-science-pack", 1], ["logistic-science-pack", 1]]}, "effects": [{"type": "unlock-recipe", "recipe": "basic-oil-processing"}]},
            "advanced-oil-processing": {"prerequisites": ["oil-processing"], "unit": {"count": 75, "time": 30, "ingredients": [["automation-science-pack", 1], ["logistic-science-pack", 1], ["chemical-science-pack", 1]]}, "effects": [{"type": "unlock-recipe", "recipe": "advanced-oil-processing"}, {"type": "unlock-recipe", "recipe": "heavy-oil-cracking"}, {"type": "unlock-recipe", "recipe": "light-oil-cracking"}]},
            "kovarex-enrichment-process": {"prerequisites": ["nuclear-power"], "unit": {"count": 1500, "time": 30, "ingredients": [["automation-science-pack", 1], ["logistic-science-pack", 1], ["chemical-science-pack", 1], ["production-science-pack", 1]]}, "effects": [{"type": "unlock-recipe", "recipe": "kovarex-enrichment-process"}]}
        }
    }

def main():
    parser = argparse.ArgumentParser(description="Factoria Build-Time Extraction Pipeline")
    parser.add_argument("--data-dump", type=str, help="Path to Factorio data-raw.json file or folder")
    parser.add_argument("--output-db", type=str, default="factorio_data_2.0.db", help="Path to output SQLite database")
    parser.add_argument("--output-manifest", type=str, default="manifest.json", help="Path to output manifest.json")
    parser.add_argument("--factorio-version", type=str, default="2.0.32", help="Factorio Version string")
    parser.add_argument("--generate-sample", action="store_true", help="Generate sample 2.0+ Space Age dataset if no dump is provided")
    
    args = parser.parse_args()
    
    data = None
    if args.data_dump and os.path.isfile(args.data_dump):
        print(f"[Factoria Pipeline] Reading prototype dump from {args.data_dump}...")
        with open(args.data_dump, "r", encoding="utf-8") as f:
            data = json.load(f)
    else:
        print("[Factoria Pipeline] No external dump provided. Generating built-in Factorio 2.0+ Space Age verified dataset...")
        data = generate_sample_factorio_dataset()
        
    print(f"[Factoria Pipeline] Initializing SQLite database schema at {args.output_db}...")
    conn = init_database(args.output_db)
    
    print("[Factoria Pipeline] Populating database tables and indexing...")
    populate_database(conn, data, factorio_version=args.factorio_version, is_space_age=True)
    
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM items")
    n_items = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM fluids")
    n_fluids = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM recipes")
    n_recipes = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM entities")
    n_entities = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM technologies")
    n_techs = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM search_index")
    n_search = cur.fetchone()[0]
    conn.close()
    
    manifest = {
        "appName": "Factoria",
        "factorioVersion": args.factorio_version,
        "spaceAgeEnabled": True,
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat().replace("+00:00", "Z"),
        "counts": {
            "items": n_items,
            "fluids": n_fluids,
            "recipes": n_recipes,
            "entities": n_entities,
            "technologies": n_techs,
            "searchEntries": n_search
        }
    }
    
    with open(args.output_manifest, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
        
    print(f"[Factoria Pipeline] Completed successfully!")
    print(f"  - Items: {n_items}")
    print(f"  - Fluids: {n_fluids}")
    print(f"  - Recipes: {n_recipes}")
    print(f"  - Entities: {n_entities}")
    print(f"  - Technologies: {n_techs}")
    print(f"  - Indexed Search Entries: {n_search}")
    print(f"  - Manifest written to {args.output_manifest}")

if __name__ == "__main__":
    main()
