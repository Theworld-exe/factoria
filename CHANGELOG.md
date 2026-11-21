# Changelog

All notable changes to **Factoria** are documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.0.0] - 2026-11-21

### Added
- Multi-module Android companion app for Factorio 2.0 & Space Age (`:app`, `:core:model`, `:core:solver`, `:core:blueprint`, `:core:database`, `:core:designsystem`).
- Custom Two-Phase Simplex linear programming production calculator supporting 4 optimization objectives, Space Age quality tiers, catalyst startup buffers, saved profiles, and plain-text plan export.
- Factorio 2.0 blueprint string decoder (`0eN...` Base64 + zlib) with a 50 MB decompression limit, Bill of Materials calculator, blueprint book flattening, and verified pre-made 2.0 blueprint library (`SampleBlueprints`).
- Offline Room SQLite database with FTS4 full-text search, Codex browser, Interactive Tech Tree with cumulative science calculation, All Recipes catalog, Enemies Bestiary, Space Age Planets compendium, 10 Logistics & Power sub-calculators, and planet-filtered Factory Todo Checklist.
- Standalone Python 3.11+ data extraction and normalization pipeline (`tools/data-pipeline`) with FactorioLab and raw prototype adapters, SQLite FK/integrity validation, query benchmarks, and `unittest` coverage.
- GitHub Actions CI workflow (`.github/workflows/ci.yml`), repository documentation, `.editorconfig`, `.gitattributes`, and `local.properties.example`.

### Fixed
- Replaced corrupted sample and pre-made library blueprint strings with verified zlib-compressed Factorio 2.0 blueprints and added 1-tap inspection alongside clipboard copy.
- Mapped oversized blueprint payloads to `BlueprintError.OVERSIZED_PAYLOAD` and rejected truncated zlib streams properly in `BlueprintDecoder`.
- Fixed duplicate target aggregation and fluid type resolution in `ProductionCalculator`.
- Fixed coroutine leak on repeated item selection in `CodexViewModel` and added search job cancellation in `CodexViewModel` and `DashboardViewModel`.
- Fixed duplicate click trigger on the unlock action in `PaywallDialog` and defensive copying in `UserPreferencesManager`.
- Added `INTERNET` permission for optional manual Data Pack HTTPS checks, wired `roundIcon` to `@mipmap/ic_launcher_round`, and ensured `HttpsURLConnection` / SQLite resources are closed in `finally` blocks in `DatabaseSyncManager`.
- Fixed division-by-zero edge case in train unloading calculator, exact `N×M` interior nuclear reactor adjacency calculation, horizontal scrolling for chip rows on narrow screens, and a typo in the 2.0 Fluids section.
- Fixed `TypeError` in `FactorioPrototypeAdapter` when parsing `module_specification.module_slots`, replaced deprecated `datetime.utcnow()`, fixed content hashing in `factorio_extractor.py`, and fixed default DB path in `benchmark_queries.py`.
- Removed duplicate asset copies from `app/src/main/assets/` in favor of `:core:database` as the single source of truth, and excluded local build outputs and machine-specific files via `.gitignore`.
