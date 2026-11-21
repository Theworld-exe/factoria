import os
import sqlite3
from typing import Dict, Any, Tuple, List
from pipeline_models import ParsedDataset

SQL_SCHEMA = """
-- Metadata key-value store
CREATE TABLE IF NOT EXISTS metadata (
    key TEXT PRIMARY KEY,
    value TEXT
);

-- Item/Recipe Categories
CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    icon TEXT
);

-- Items and Fluids
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category_id TEXT,
    stack_size INTEGER DEFAULT 1,
    icon TEXT,
    type TEXT, -- item or fluid
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Machines and Entities
CREATE TABLE IF NOT EXISTS machines (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    crafting_speed REAL NOT NULL,
    energy_usage_kw REAL NOT NULL,
    pollution_per_minute REAL DEFAULT 0,
    module_slots INTEGER DEFAULT 0,
    icon TEXT
);

-- Recipes
CREATE TABLE IF NOT EXISTS recipes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category_id TEXT,
    crafting_time REAL NOT NULL,
    is_catalyst BOOLEAN DEFAULT 0,
    icon TEXT
);

-- Recipe Ingredients
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    recipe_id TEXT NOT NULL,
    item_id TEXT NOT NULL,
    amount REAL NOT NULL,
    PRIMARY KEY (recipe_id, item_id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Recipe Products
CREATE TABLE IF NOT EXISTS recipe_products (
    recipe_id TEXT NOT NULL,
    item_id TEXT NOT NULL,
    amount REAL NOT NULL,
    probability REAL DEFAULT 1.0,
    PRIMARY KEY (recipe_id, item_id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Machine Crafting Capability Mapping
CREATE TABLE IF NOT EXISTS machine_recipes (
    machine_id TEXT NOT NULL,
    recipe_category TEXT NOT NULL,
    PRIMARY KEY (machine_id, recipe_category)
);

-- Required Baseline Indexes
CREATE INDEX IF NOT EXISTS idx_items_name ON items(name);
CREATE INDEX IF NOT EXISTS idx_ingredients_item ON recipe_ingredients(item_id);
CREATE INDEX IF NOT EXISTS idx_products_item ON recipe_products(item_id);

-- Additional Justified Performance Indexes
-- 1. Accelerates filtering recipes by category (e.g. intermediate products, logistics)
CREATE INDEX IF NOT EXISTS idx_recipes_category ON recipes(category_id);
-- 2. Accelerates reverse lookups: finding machines capable of crafting a specific recipe category
CREATE INDEX IF NOT EXISTS idx_machine_recipes_category ON machine_recipes(recipe_category);
-- 3. Accelerates codex filtering for items vs fluids (e.g. WHERE type = 'fluid')
CREATE INDEX IF NOT EXISTS idx_items_type ON items(type);
-- 4. Accelerates listing items belonging to a specific item-group category
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
"""

class DatabaseBuilder:
    """Builds and validates normalized SQLite databases from ParsedDataset models."""

    @staticmethod
    def build(dataset: ParsedDataset, output_path: str) -> Dict[str, Any]:
        """
        Creates, populates, indexes, and verifies the SQLite database.
        Uses WAL mode and enforces foreign keys.
        """
        # Ensure parent directory exists
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        if os.path.exists(output_path):
            os.remove(output_path)

        conn = sqlite3.connect(output_path)
        try:
            conn.execute("PRAGMA foreign_keys = ON;")
            conn.execute("PRAGMA journal_mode = WAL;")
            conn.execute("PRAGMA synchronous = NORMAL;")

            # Initialize schema
            conn.executescript(SQL_SCHEMA)

            with conn:
                # 1. Insert Metadata
                conn.executemany(
                    "INSERT INTO metadata (key, value) VALUES (?, ?);",
                    list(dataset.metadata.items())
                )

                # 2. Insert Categories
                conn.executemany(
                    "INSERT INTO categories (id, name, icon) VALUES (?, ?, ?);",
                    [(c.id, c.name, c.icon) for c in dataset.categories]
                )

                # 3. Insert Items
                conn.executemany(
                    "INSERT INTO items (id, name, category_id, stack_size, icon, type) VALUES (?, ?, ?, ?, ?, ?);",
                    [(it.id, it.name, it.category_id, it.stack_size, it.icon, it.type) for it in dataset.items]
                )

                # 4. Insert Machines
                conn.executemany(
                    "INSERT INTO machines (id, name, category, crafting_speed, energy_usage_kw, pollution_per_minute, module_slots, icon) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                    [(m.id, m.name, m.category, m.crafting_speed, m.energy_usage_kw, m.pollution_per_minute, m.module_slots, m.icon) for m in dataset.machines]
                )

                # 5. Insert Recipes
                conn.executemany(
                    "INSERT INTO recipes (id, name, category_id, crafting_time, is_catalyst, icon) VALUES (?, ?, ?, ?, ?, ?);",
                    [(r.id, r.name, r.category_id, r.crafting_time, 1 if r.is_catalyst else 0, r.icon) for r in dataset.recipes]
                )

                # 6. Insert Recipe Ingredients
                ing_tuples = []
                for r in dataset.recipes:
                    for ing in r.ingredients:
                        ing_tuples.append((r.id, ing.item_id, ing.amount))
                conn.executemany(
                    "INSERT OR REPLACE INTO recipe_ingredients (recipe_id, item_id, amount) VALUES (?, ?, ?);",
                    ing_tuples
                )

                # 7. Insert Recipe Products
                prod_tuples = []
                for r in dataset.recipes:
                    for prod in r.products:
                        prod_tuples.append((r.id, prod.item_id, prod.amount, prod.probability))
                conn.executemany(
                    "INSERT OR REPLACE INTO recipe_products (recipe_id, item_id, amount, probability) VALUES (?, ?, ?, ?);",
                    prod_tuples
                )

                # 8. Insert Machine Recipes
                conn.executemany(
                    "INSERT OR IGNORE INTO machine_recipes (machine_id, recipe_category) VALUES (?, ?);",
                    [(mr.machine_id, mr.recipe_category) for mr in dataset.machine_recipes]
                )

            # Verification Phase
            validation_report = DatabaseBuilder.validate(conn)
            return validation_report

        finally:
            conn.close()

    @staticmethod
    def validate(conn: sqlite3.Connection) -> Dict[str, Any]:
        """Runs SQLite integrity checks, foreign key checks, and row counts."""
        cursor = conn.cursor()

        # 1. Integrity Check
        cursor.execute("PRAGMA integrity_check;")
        integrity_rows = cursor.fetchall()
        is_integrity_ok = len(integrity_rows) == 1 and integrity_rows[0][0] == "ok"

        # 2. Foreign Key Check
        cursor.execute("PRAGMA foreign_key_check;")
        fk_violations = cursor.fetchall()

        # 3. Row Counts
        counts = {}
        for tbl in ["metadata", "categories", "items", "machines", "recipes", "recipe_ingredients", "recipe_products", "machine_recipes"]:
            cursor.execute(f"SELECT COUNT(*) FROM {tbl};")
            counts[tbl] = cursor.fetchone()[0]

        report = {
            "integrity_ok": is_integrity_ok,
            "integrity_output": [r[0] for r in integrity_rows],
            "foreign_key_violations_count": len(fk_violations),
            "foreign_key_violations": fk_violations,
            "counts": counts,
            "is_valid": is_integrity_ok and (len(fk_violations) == 0) and (counts["items"] > 0) and (counts["recipes"] > 0)
        }

        if not report["is_valid"]:
            raise ValueError(f"Database validation failed: {report}")

        return report
