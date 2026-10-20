# Release process (no secrets required until publish)

Tagging and Central Portal upload are **human** acts. Agents never tag or publish.

## Checklist before a tag

1. `main` is green: `./gradlew check`
2. Local coordinates install: `./gradlew publishToMavenLocal`
3. [CHANGELOG.md](../CHANGELOG.md) has an `[Unreleased]` section ready to become a version heading
4. Bump `version=` in `gradle.properties` to the release number (must match the tag without `v`)
5. Open a PR for the version bump + changelog; merge when green

## Tag and publish (human + secrets)

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

## Without Central secrets (GitHub-only story)

You can still:

- Develop and CI against `999-SNAPSHOT`
- `publishToMavenLocal` for local consumers (`./gradlew publishToMavenLocal`)
- Cut a **source** GitHub release without Portal upload:
  1. Bump `version=` + changelog PR → merge
  2. Human tags `v$VERSION` (agents never tag)
  3. `publish.yml` **fails closed** without `SIGNING_*` / `MAVEN_CENTRAL_*` — that is expected
  4. Create a GitHub Release from the tag in the UI (or `gh release create`) attaching nothing, or
     attaching locally built jars from `./gradlew :quarkus-connect:assemble :quarkus-connect-deployment:assemble`
  5. Consumers depend on JitPack / source build / `mavenLocal` until Central secrets exist

There is no agent-driven publish path; fail-closed Central is intentional.

## Scope gates for a first release

| Ready | Item |
| --- | --- |
| Yes | Unary Connect + gRPC-Web + server/client/bidi streaming for `@GrpcService` |
| Yes | Examples + Flutter client + SPI docs |
| Yes | Publish plugin + fail-closed workflow |
| Human | Repository secrets + Claude Code app (optional for agent automation) |
| Later | Interactive/chunked stream bodies; brotli/zstd message coding |
