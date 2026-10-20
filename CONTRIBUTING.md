# Contributing

Thank you for considering a contribution.

## Before you start

1. Read [AGENTS.md](AGENTS.md) — scope, merging rules, and what trusted authors may ask for.
2. Open an issue for non-trivial changes unless you are fixing a clear bug.
3. Keep public names generic (no private product codenames).

## Build

```bash
./gradlew check
```

JDK **21** builds the project; published bytecode targets **JVM 17**.

Examples:

```bash
./gradlew :examples:quarkus-kotlin-server:quarkusDev
cd examples/flutter-web-client && flutter pub get && flutter analyze
```

## Pull requests

- Branch from `main`; prefer squash merge.
- Paths under `.github/`, `AGENTS.md`, root `build.gradle.kts`, and `gradle.properties`
  require `@lldata` review (CODEOWNERS).
- Do not tag or publish from a PR. Releases are human-driven (`docs/RELEASE.md`).

## License

By contributing you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
