import argparse
import json
import os
import sys
from datetime import datetime, timezone

from source_adapter import FactorioLabAdapter, FactorioPrototypeAdapter
from database_builder import DatabaseBuilder
from benchmark_queries import run_benchmarks

def main():
    parser = argparse.ArgumentParser(
        description="Factoria Data Pipeline: Build-time ingestion CLI for Factorio 2.0+ datasets."
    )
    parser.add_argument(
        "--source",
        choices=["factoriolab", "prototype"],
        default="factoriolab",
        help="Source format adapter to use (default: factoriolab)."
    )
    parser.add_argument(
        "--dataset",
        default="2.0",
        help="Dataset identifier (e.g., '2.0', 'spa', '1.1') or local file path (default: '2.0')."
    )
    parser.add_argument(
        "--url",
        default=None,
        help="Explicit custom URL or pinned commit raw GitHub URL to fetch data from."
    )
    parser.add_argument(
        "--difficulty",
        choices=["normal", "expensive"],
        default="normal",
        help="Recipe difficulty variant to extract if present in prototype source (default: 'normal')."
    )
    parser.add_argument(
        "--output",
        default="tools/data-pipeline/factorio_default.db",
        help="Path for output SQLite database file (default: tools/data-pipeline/factorio_default.db)."
    )
    parser.add_argument(
        "--benchmark",
        action="store_true",
        help="Run query performance benchmarks on the generated database."
    )
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="Validate an existing SQLite database file without fetching or building."
    )

    args = parser.parse_args()

    if args.validate_only:
        import sqlite3
        if not os.path.exists(args.output):
            print(f"Error: Database file does not exist: {args.output}")
            sys.exit(1)
        print(f"Validating database: {args.output}")
        conn = sqlite3.connect(args.output)
        try:
            report = DatabaseBuilder.validate(conn)
            print("Validation PASSED:")
            print(json.dumps(report, indent=2))
        finally:
            conn.close()
        return

    print(f"============================================================")
    print(f" Factoria Data Pipeline CLI")
    print(f" Source: {args.source} | Dataset: {args.dataset}")
    print(f" Target Output: {args.output}")
    print(f"============================================================")

    # 1. Initialize Adapter
    if args.source == "factoriolab":
        adapter = FactorioLabAdapter()
    else:
        adapter = FactorioPrototypeAdapter(difficulty=args.difficulty)

    # 2. Fetch
    print(f"[1/4] Fetching game data from source...")
    payload, resolved_url, checksum = adapter.fetch(args.dataset, custom_url=args.url)
    print(f"      Resolved URL: {resolved_url}")
    print(f"      Content SHA-256: {checksum}")

    # 3. Parse & Transform
    print(f"[2/4] Transforming and normalizing data...")
    metadata_overrides = {
        "dataset": args.dataset,
        "source_url": resolved_url,
        "content_checksum": checksum,
        "fetch_timestamp": datetime.now(timezone.utc).isoformat()
    }
    dataset = adapter.parse(payload, metadata_overrides=metadata_overrides)
    print(f"      Parsed: {len(dataset.categories)} categories, {len(dataset.items)} items, "
          f"{len(dataset.machines)} machines, {len(dataset.recipes)} recipes, "
          f"{len(dataset.machine_recipes)} machine-recipe mappings.")

    # 4. Build SQLite Database
    print(f"[3/4] Building SQLite database with foreign-key enforcement...")
    report = DatabaseBuilder.build(dataset, args.output)
    print(f"      Integrity check: {report['integrity_output']}")
    print(f"      Foreign key violations: {report['foreign_key_violations_count']}")
    print(f"      Row counts: {report['counts']}")
    print(f"      Database built successfully at: {args.output}")

    # 5. Optional Benchmark
    if args.benchmark:
        print(f"\n[4/4] Executing query performance benchmarks...")
        bench_results = run_benchmarks(args.output, iterations=100)
        for b in bench_results:
            print(f"  * {b['name']}: avg = {b['avg_ms']} ms ({b['avg_us']} us), p95 = {b['p95_ms']} ms")

    print("\n[SUCCESS] Pipeline execution finished cleanly.")

if __name__ == "__main__":
    main()
