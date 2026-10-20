---
name: release-readiness
description: Weekly — say whether a release is due; never tag or publish.
---

# Release readiness

Read `AGENTS.md` first. This never tags and never publishes. Tagging and Central Portal
upload are human acts (`publish.yml` on a `v*` tag).

## Assess

1. Compare `main` since the last tag (`git describe`, `git log <tag>..main`).
2. Check `gradle.properties` `version=` — still `999-SNAPSHOT` means no release number is
   committed yet.
3. Confirm `./gradlew check` and `publishToMavenLocal` still work.
4. Look for half-finished work: open PRs, `needs-decision` issues, failing agent sweeps.

## Report

Open or update a single standing issue titled exactly `Release readiness` only when a release
is actually due. Otherwise comment that nothing is due and stop.

When due, list what would ship, what blockers remain (secrets, version bump, changelog), and
stop. Do not bump the version or push a tag.
