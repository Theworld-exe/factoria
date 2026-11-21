import sqlite3
import time
from typing import Dict, Any, List

BENCHMARK_QUERIES = [
    (
        "Search items by prefix name (uses idx_items_name)",
        "SELECT id, name, category_id, stack_size, type FROM items WHERE name LIKE 'Iron%' LIMIT 20;",
        ()
    ),
    (
        "Filter items by type (uses idx_items_type)",
        "SELECT id, name, category_id FROM items WHERE type = 'fluid';",
        ()
    ),
    (
        "Lookup recipes producing specific item (uses idx_products_item)",
        """
        SELECT r.id, r.name, r.crafting_time, rp.amount, rp.probability
        FROM recipes r
        JOIN recipe_products rp ON r.id = rp.recipe_id
        WHERE rp.item_id = 'iron-plate';
        """,
        ()
    ),
    (
        "Lookup recipes consuming specific item (uses idx_ingredients_item)",
        """
        SELECT r.id, r.name, ri.amount
        FROM recipes r
        JOIN recipe_ingredients ri ON r.id = ri.recipe_id
        WHERE ri.item_id = 'iron-plate';
        """,
        ()
    ),
    (
        "Find machines capable of crafting a specific recipe (uses idx_machine_recipes_category)",
        """
        SELECT m.id, m.name, m.crafting_speed, m.energy_usage_kw
        FROM machines m
        JOIN machine_recipes mr ON m.id = mr.machine_id
        JOIN recipes r ON r.category_id = mr.recipe_category
        WHERE r.id = 'electronic-circuit';
        """,
        ()
    ),
    (
        "Full recipe bill of materials (joins ingredients, products, items)",
        """
        SELECT 
            r.id AS recipe_id,
            r.name AS recipe_name,
            r.crafting_time,
            ri.item_id AS ing_id,
            ing_item.name AS ing_name,
            ri.amount AS ing_amount,
            rp.item_id AS prod_id,
            prod_item.name AS prod_name,
            rp.amount AS prod_amount
        FROM recipes r
        LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_id
        LEFT JOIN items ing_item ON ri.item_id = ing_item.id
        LEFT JOIN recipe_products rp ON r.id = rp.recipe_id
        LEFT JOIN items prod_item ON rp.item_id = prod_item.id
        WHERE r.id = 'advanced-oil-processing';
        """,
        ()
    )
]

def run_benchmarks(db_path: str, iterations: int = 100) -> List[Dict[str, Any]]:
    """Runs realistic application queries against the SQLite database and measures latency."""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    results = []

    for name, sql, params in BENCHMARK_QUERIES:
        # 1. Fetch EXPLAIN QUERY PLAN
        cursor.execute(f"EXPLAIN QUERY PLAN {sql}", params)
        plan_rows = [row[3] for row in cursor.fetchall()]

        # 2. Warm up
        for _ in range(5):
            cursor.execute(sql, params)
            cursor.fetchall()

        # 3. Timed runs
        durations_us = []
        for _ in range(iterations):
            start = time.perf_counter_ns()
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            end = time.perf_counter_ns()
            durations_us.append((end - start) / 1000.0)

        durations_us.sort()
        avg_us = sum(durations_us) / len(durations_us)
        p50_us = durations_us[len(durations_us) // 2]
        p95_us = durations_us[int(len(durations_us) * 0.95)]
        max_us = max(durations_us)

        results.append({
            "name": name,
            "sql": sql.strip(),
            "query_plan": plan_rows,
            "row_count": len(rows),
            "iterations": iterations,
            "avg_us": round(avg_us, 2),
            "p50_us": round(p50_us, 2),
            "p95_us": round(p95_us, 2),
            "max_us": round(max_us, 2),
            "avg_ms": round(avg_us / 1000.0, 4),
            "p95_ms": round(p95_us / 1000.0, 4)
        })

    conn.close()
    return results

if __name__ == "__main__":
    import sys
    db = sys.argv[1] if len(sys.argv) > 1 else "tools/data-pipeline/factorio_default.db"
    res = run_benchmarks(db)
    print(f"\n--- SQLite Query Benchmark Results ({db}) ---")
    for r in res:
        print(f"\n[Query]: {r['name']}")
        print(f"  Plan: {', '.join(r['query_plan'])}")
        print(f"  Rows: {r['row_count']} | Iterations: {r['iterations']}")
        print(f"  Latency: avg={r['avg_ms']} ms ({r['avg_us']} us), p50={r['p50_us']} us, p95={r['p95_ms']} ms ({r['p95_us']} us)")
