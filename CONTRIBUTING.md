# Contributing to Factoria

Thank you for your interest in contributing to **Factoria**! Whether you are fixing a solver edge case, improving Compose UI responsiveness, or expanding the Python data pipeline, this guide will help you get started.

## Getting Started

1. Fork the repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/factoria.git
   cd factoria
   ```
2. Configure your local Android SDK path by copying `local.properties.example` to `local.properties` and setting `sdk.dir`.
3. Create a descriptive feature or bug-fix branch from `main`:
   ```bash
   git checkout -b fix/blueprint-decoder-edge-case
   ```

## Development Guidelines

### Kotlin & Android
- Follow the official Kotlin coding style (`kotlin.code.style=official`) and the formatting rules in `.editorconfig`.
- Keep pure business logic (linear programming, blueprint decoding, domain models) inside the pure JVM modules (`:core:model`, `:core:solver`, `:core:blueprint`) free of Android framework imports so it remains fast to test on any JVM.
- Preserve Factoria's offline-first architecture: core features must never require network access.
- Avoid introducing unnecessary third-party dependencies when Kotlin/AndroidX/Compose APIs already solve the problem cleanly.

### Python Data Pipeline (`tools/data-pipeline`)
- Target Python 3.11+ using standard library modules only unless a new dependency is explicitly justified.
- Ensure every generated SQLite database passes `PRAGMA integrity_check` and `PRAGMA foreign_key_check`.

## Running Checks Before Opening a Pull Request

Run the full test and build verification suite locally:

```bash
# Kotlin / JVM unit tests
./gradlew test --console=plain

# Debug APK build
./gradlew assembleDebug --console=plain

# Android Lint
./gradlew lintDebug --console=plain

# Python data pipeline tests
python -m unittest discover -s tools/data-pipeline/tests -v
```

## Pull Request Checklist

- [ ] Changes are focused and scoped to a single feature or fix.
- [ ] New or modified behavior includes unit tests where applicable.
- [ ] All Gradle tests, debug build, lint checks, and Python unit tests pass.
- [ ] No secrets, keystores, `local.properties`, compiled `.apk`/`.aab` files, or generated `.db` artifacts outside `core/database/src/main/assets/` are staged.
- [ ] Documentation (`README.md`, `CHANGELOG.md`) is updated if behavior or commands changed.

## Code of Conduct

All contributors are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).
