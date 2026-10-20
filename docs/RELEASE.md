# Release process (no secrets required until publish)

Tagging and Central Portal upload are **human** acts. Agents never tag or publish.

Recommended first public version: **`0.1.0`** (semver; still early). Stay on `999-SNAPSHOT` until that bump PR merges.

## Checklist before a tag

1. `main` is green: `./gradlew check`
2. Local coordinates install: `./gradlew publishToMavenLocal`
3. [CHANGELOG.md](../CHANGELOG.md) has an `[Unreleased]` section ready to become `## [0.1.0] - YYYY-MM-DD` (or the chosen version)
4. Bump `version=` in `gradle.properties` to the release number (must match the tag without `v`)
5. Open a PR for the version bump + changelog; merge when green

## Path A — GitHub-only (no Central secrets)

Use this until `SIGNING_*` / `MAVEN_CENTRAL_*` exist. `publish.yml` **fails closed** on the tag — that is expected and not a blocker for a GitHub Release.

```bash
# After the version-bump PR is on main:
./scripts/package-github-release.sh 0.1.0
# Script prints the exact `gh release create` command with staged jars + notes.

git tag v0.1.0
git push origin v0.1.0
# Then run the printed gh release create (or attach jars in the GitHub UI).
```

Consumers until Central:

| Path | How |
| --- | --- |
| `mavenLocal` | `./gradlew publishToMavenLocal` then depend on `io.github.lldata:quarkus-connect:0.1.0` |
| Source / composite build | Clone tag; `includeBuild` or publishToMavenLocal from the tag |
| GitHub Release jars | Download attached jars from the release page (not a Maven repo) |

There is no agent-driven publish path.

## Path B — Tag + Maven Central (human + secrets)

```bash
git tag "v$VERSION"
git push origin "v$VERSION"
```

`.github/workflows/publish.yml` then:

1. Fails closed if `SIGNING_*` / `MAVEN_CENTRAL_*` secrets are missing
2. Runs `./gradlew check` and `publishToMavenCentral`
3. Polls the Central Portal until VALIDATED (or fails with Portal errors)
4. Opens a **draft** GitHub release with the jars

A human presses **Publish** on [central.sonatype.com/publishing](https://central.sonatype.com/publishing).
There is no automatic release to Maven Central.

## Scope gates for a first release

| Ready | Item |
| --- | --- |
| Yes | Unary Connect + gRPC-Web + server/client/bidi streaming for `@GrpcService` |
| Yes | Examples + Flutter client + SPI docs |
| Yes | Curl interop matrix + partial connectconformance host in CI |
| Yes | Publish plugin + fail-closed workflow + GitHub-only packaging script |
| Human | Repository secrets (Central) + Claude Code app (optional for agent automation) |
| Later | Interactive/chunked stream bodies; brotli/zstd; full conformance matrix |
