# Release process

## Model

| Path | What runs | When |
| --- | --- | --- |
| **CI** | `./gradlew check`; on `main` only, `publishToMavenLocal` + upload `snapshot-maven-repo` | Every PR / push — **never** Central publish |
| **Publish** | Promote newest main SNAPSHOT artifact → release version, Central upload, tag, draft GH release | **`workflow_dispatch` only** (manual or AI) |

The Publish workflow **does not rebuild**. It downloads the `snapshot-maven-repo` artifact from the successful CI run for current `main` HEAD, rewrites `X.Y.Z-SNAPSHOT` → `X.Y.Z`, signs with Environment `publish` secrets, and uploads a Central Portal bundle (`USER_MANAGED`).

Recommended first Central version follows the next SNAPSHOT on `main` (today: promote `0.1.7-SNAPSHOT` → `0.1.7`).

## Checklist before promoting

1. `main` is green and `version=` in `gradle.properties` is `*-SNAPSHOT`
2. CI for that commit uploaded `snapshot-maven-repo` (push to `main` only)
3. [CHANGELOG.md](../CHANGELOG.md) `[Unreleased]` describes what will ship
4. GitHub Environment **`publish`** has non-empty `SIGNING_KEY`, `SIGNING_PASSWORD`, `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`

## Trigger Publish (manual / AI)

Workflow name: **Publish** (`.github/workflows/publish.yml`).

```bash
# Actions UI: Actions → Publish → Run workflow → confirm = promote

gh workflow run Publish \
  --repo lldata/quarkus-connect \
  -f confirm=promote
```

The run then:

1. Fails closed if Environment `publish` secrets are missing
2. Resolves newest snapshot from `main` `gradle.properties` (refuses non-SNAPSHOT or if `v$release` exists)
3. Downloads CI `snapshot-maven-repo` for that exact commit (refuses older runs)
4. Promotes / signs / uploads to the Central Portal (no `./gradlew` build)
5. Tags `v$release` at that commit and opens a **draft** GitHub release with jars
6. Opens a post-release PR bumping `main` to the next patch `*-SNAPSHOT`

A human presses **Publish** on [central.sonatype.com/publishing](https://central.sonatype.com/publishing).
There is no automatic release to Maven Central.

## Recovery — Central VALIDATED, tag missing

If Portal upload reached **VALIDATED** but the annotated tag / draft GitHub release step
failed (for example empty `user.name` / `user.email` before `git tag -a`), **do not**
re-run a full promote. That would upload a second Central deployment for the same
version.

After this workflow fix is on `main`, finish GitHub-only (no Portal upload):

```bash
gh workflow run Publish \
  --repo lldata/quarkus-connect \
  -f confirm=promote \
  -f github_only=true
```

That path still downloads the CI snapshot, rewrites jars for the draft release
(`--skip-sign`), creates `v$release` + draft GH release, and opens the post-release
SNAPSHOT bump PR. Press **Publish** on the existing Portal deployment when ready.

If `v$release` already exists, Publish refuses to re-release — create or edit the
draft GH release by hand if only that piece is missing.

## Path A — GitHub-only (no Central secrets)

Use only while Environment `publish` secrets are incomplete. Prefer the packaging script; do **not** run Publish until signing + Central credentials exist.

```bash
./scripts/package-github-release.sh 0.1.0
# Script prints gh release create. Tagging is still a deliberate act.
```

| Path | How |
| --- | --- |
| `mavenLocal` | `./gradlew publishToMavenLocal` then depend on the version |
| Source / composite build | Clone tag; `includeBuild` or publishToMavenLocal from source |
| GitHub Release jars | Download attached jars (not a Maven repo) |

## Scope gates

| Ready | Item |
| --- | --- |
| Yes | Unary Connect + gRPC-Web + server/client/bidi streaming for `@GrpcService` |
| Yes | Examples + Flutter client + SPI docs |
| Yes | Curl interop matrix + partial connectconformance host in CI |
| Yes | Snapshot staging on main CI + promote-only Publish workflow |
| Human | Environment `publish` secrets (Central + GPG) |
| Later | Interactive/chunked stream bodies; brotli/zstd; full conformance matrix |
