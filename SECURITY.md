# Security Policy

## Supported Versions

Security fixes are applied to the latest state of the `main` branch.

| Version | Supported |
| --- | --- |
| 2.0.x (`main`) | Yes |

## Security Architecture

Factoria is designed to minimize attack surface:

- **Offline-First**: Core app functionality operates entirely on bundled local SQLite assets without accounts, analytics SDKs, or mandatory network calls.
- **Blueprint Decompression Guard**: `BlueprintDecoder` enforces a strict uncompressed payload cap (`50 MB` default) while streaming zlib inflation to prevent decompression-bomb denial-of-service attacks from untrusted blueprint strings.
- **Verified Optional Data Sync**: `DatabaseSyncManager` enforces HTTPS-only manifest and dataset downloads, verifies SHA-256 digests before opening downloaded SQLite files, runs `PRAGMA integrity_check` and `PRAGMA foreign_key_check`, and performs atomic replacement with automatic rollback on failure.
- **Parameterized Queries**: Room DAOs and SQLite fallback queries use bound parameters (`?`) rather than raw string interpolation for user input.

## Reporting a Vulnerability

If you discover a security vulnerability in Factoria, please **do not** open a public GitHub issue containing exploit details.

Instead:
1. Use GitHub's **Security Advisories / Private Vulnerability Reporting** feature on this repository if enabled, or contact the maintainer directly via their GitHub profile: https://github.com/Theworld-exe.
2. Include a clear description of the issue, reproduction steps, affected files or versions, and any suggested remediation.

We will acknowledge receipt as soon as possible and work with you to verify and address the report before public disclosure.
