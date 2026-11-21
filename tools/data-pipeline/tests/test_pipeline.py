import json
import os
import sqlite3
import tempfile
import unittest

import sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from source_adapter import FactorioLabAdapter, FactorioPrototypeAdapter, parse_energy_to_kw
from database_builder import DatabaseBuilder
import factorio_extractor

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "fixtures")

class TestPipeline(unittest.TestCase):

    def setUp(self):
        f_path = os.path.join(FIXTURES_DIR, "factoriolab_sample.json")
        with open(f_path, "r", encoding="utf-8") as f:
            self.factoriolab_payload = json.load(f)

        p_path = os.path.join(FIXTURES_DIR, "prototype_sample.json")
        with open(p_path, "r", encoding="utf-8") as f:
            self.prototype_payload = json.load(f)

    def test_factoriolab_adapter_parsing(self):
        adapter = FactorioLabAdapter()
        dataset = adapter.parse(self.factoriolab_payload)

        # 1. Categories
        cat_ids = {c.id for c in dataset.categories}
        self.assertIn("logistics", cat_ids)
        self.assertIn("intermediate-products", cat_ids)
        self.assertIn("unknown", cat_ids)

        # 2. Items & Fluid differentiation
        item_map = {it.id: it for it in dataset.items}
        self.assertEqual(item_map["wooden-chest"].type, "item")
        self.assertEqual(item_map["wooden-chest"].stack_size, 50)
        self.assertEqual(item_map["water"].type, "fluid")
        self.assertEqual(item_map["crude-oil"].type, "fluid")

        # 3. Machines
        machine_map = {m.id: m for m in dataset.machines}
        self.assertIn("assembling-machine-1", machine_map)
        self.assertEqual(machine_map["assembling-machine-1"].crafting_speed, 0.5)
        self.assertEqual(machine_map["assembling-machine-1"].energy_usage_kw, 75.0)
        self.assertEqual(machine_map["assembling-machine-1"].pollution_per_minute, 4.0)

        # Passive machine check (offshore pump has 0 kW)
        self.assertIn("offshore-pump", machine_map)
        self.assertEqual(machine_map["offshore-pump"].energy_usage_kw, 0.0)

        # 4. Recipes
        recipe_map = {r.id: r for r in dataset.recipes}
        
        # Kovarex catalyst
        kovarex = recipe_map["kovarex-enrichment-process"]
        self.assertTrue(kovarex.is_catalyst)
        self.assertEqual(kovarex.crafting_time, 60.0)
        
        # Uranium processing fractional outputs
        uranium = recipe_map["uranium-processing"]
        prod_map = {p.item_id: p.amount for p in uranium.products}
        self.assertEqual(prod_map["uranium-235"], 0.007)
        self.assertEqual(prod_map["uranium-238"], 0.993)

        # Default crafting time (omitted in quick-craft-recipe -> 0.5s default)
        quick = recipe_map["quick-craft-recipe"]
        self.assertEqual(quick.crafting_time, 0.5)

    def test_factoriolab_database_build_and_foreign_keys(self):
        adapter = FactorioLabAdapter()
        dataset = adapter.parse(self.factoriolab_payload)

        with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
            tmp_path = tmp.name

        try:
            report = DatabaseBuilder.build(dataset, tmp_path)
            self.assertTrue(report["is_valid"])
            self.assertEqual(report["foreign_key_violations_count"], 0)
            self.assertTrue(report["integrity_ok"])
            self.assertEqual(report["counts"]["recipes"], 5)
            self.assertGreaterEqual(report["counts"]["categories"], 4)
            self.assertGreaterEqual(report["counts"]["items"], 14)
            self.assertGreaterEqual(report["counts"]["machines"], 3)

            # Verify indices exist in sqlite_master
            conn = sqlite3.connect(tmp_path)
            cursor = conn.cursor()
            cursor.execute("SELECT name FROM sqlite_master WHERE type='index';")
            indices = {row[0] for row in cursor.fetchall()}
            conn.close()

            self.assertIn("idx_items_name", indices)
            self.assertIn("idx_ingredients_item", indices)
            self.assertIn("idx_products_item", indices)
            self.assertIn("idx_recipes_category", indices)
            self.assertIn("idx_machine_recipes_category", indices)
            self.assertIn("idx_items_type", indices)
            self.assertIn("idx_items_category", indices)
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    def test_prototype_adapter_difficulty_variants(self):
        # Normal Difficulty
        adapter_norm = FactorioPrototypeAdapter(difficulty="normal")
        data_norm = adapter_norm.parse(self.prototype_payload)
        recipe_norm = {r.id: r for r in data_norm.recipes}["electronic-circuit"]
        self.assertEqual(recipe_norm.crafting_time, 0.5)
        ing_map_norm = {ing.item_id: ing.amount for ing in recipe_norm.ingredients}
        self.assertEqual(ing_map_norm["iron-plate"], 1.0)
        self.assertEqual(ing_map_norm["copper-cable"], 3.0)

        # Expensive Difficulty
        adapter_exp = FactorioPrototypeAdapter(difficulty="expensive")
        data_exp = adapter_exp.parse(self.prototype_payload)
        recipe_exp = {r.id: r for r in data_exp.recipes}["electronic-circuit"]
        self.assertEqual(recipe_exp.crafting_time, 1.0)
        ing_map_exp = {ing.item_id: ing.amount for ing in recipe_exp.ingredients}
        self.assertEqual(ing_map_exp["iron-plate"], 2.0)
        self.assertEqual(ing_map_exp["copper-cable"], 8.0)

    def test_reference_resolution_and_missing_items(self):
        # prototype_sample.json has unregistered items in synthetic-oil-cracking
        adapter = FactorioPrototypeAdapter()
        data = adapter.parse(self.prototype_payload)

        # Reference resolver must have created entries in items
        item_ids = {it.id for it in data.items}
        self.assertIn("petroleum-gas-unregistered", item_ids)
        self.assertIn("heavy-oil-unregistered", item_ids)

        # Build DB and verify foreign keys pass without violation
        with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
            tmp_path = tmp.name

        try:
            report = DatabaseBuilder.build(data, tmp_path)
            self.assertEqual(report["foreign_key_violations_count"], 0)
            self.assertTrue(report["is_valid"])
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    def test_energy_parsing_utility(self):
        self.assertEqual(parse_energy_to_kw("75kW"), 75.0)
        self.assertEqual(parse_energy_to_kw("150 kW"), 150.0)
        self.assertEqual(parse_energy_to_kw("3.5MW"), 3500.0)
        self.assertEqual(parse_energy_to_kw("200J"), 0.2)
        self.assertEqual(parse_energy_to_kw("10MJ"), 10000.0)
        self.assertEqual(parse_energy_to_kw(0), 0.0)
        self.assertEqual(parse_energy_to_kw(None), 0.0)

    def test_prototype_adapter_module_specification_dict(self):
        payload = {
            "version": "2.0.32",
            "item-group": {"production": {"name": "Production"}},
            "assembling-machine": {
                "assembling-machine-3": {
                    "crafting_speed": 1.25,
                    "energy_usage": "375kW",
                    "module_specification": {"module_slots": 4},
                    "crafting_categories": ["crafting"]
                }
            },
            "recipe": {}
        }
        adapter = FactorioPrototypeAdapter()
        dataset = adapter.parse(payload)
        machine = {m.id: m for m in dataset.machines}["assembling-machine-3"]
        self.assertEqual(machine.module_slots, 4)

    def test_factorio_extractor_sample_database(self):
        with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
            tmp_path = tmp.name

        try:
            sample = factorio_extractor.generate_sample_factorio_dataset()
            conn = factorio_extractor.init_database(tmp_path)
            factorio_extractor.populate_database(conn, sample, factorio_version="2.0.32", is_space_age=True)
            cur = conn.cursor()
            cur.execute("PRAGMA integrity_check;")
            self.assertEqual(cur.fetchone()[0], "ok")
            cur.execute("SELECT COUNT(*) FROM items;")
            self.assertGreater(cur.fetchone()[0], 0)
            cur.execute("SELECT COUNT(*) FROM recipes;")
            self.assertGreater(cur.fetchone()[0], 0)
            cur.execute("SELECT data_hash FROM manifest WHERE id = 1;")
            self.assertEqual(len(cur.fetchone()[0]), 64)
            conn.close()
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

if __name__ == "__main__":
    unittest.main()
