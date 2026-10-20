#!/usr/bin/env bash
# Build published jars for a GitHub-only release (no Maven Central / signing secrets).
# Agents never tag; a human runs the printed `gh release create` (or uses the UI).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${1:-}"
if [[ -z "$VERSION" ]]; then
  VERSION="$(sed -n 's/^version=//p' gradle.properties)"
fi
if [[ "$VERSION" == *SNAPSHOT* ]]; then
  echo "Refusing SNAPSHOT version '$VERSION'." >&2
  echo "Bump version= in gradle.properties (and CHANGELOG) on a PR first, then re-run:" >&2
  echo "  ./scripts/package-github-release.sh 0.1.0" >&2
  exit 1
fi

TAG="v${VERSION}"
OUT="$ROOT/build/github-release/${TAG}"
rm -rf "$OUT"
mkdir -p "$OUT"

echo "Building published modules for $TAG…" >&2
./gradlew \
  :quarkus-connect:assemble \
  :quarkus-connect-deployment:assemble \
  --quiet

# Prefer explicit jar names from the two published modules.
shopt -s nullglob
jars=(
  runtime/build/libs/quarkus-connect-"${VERSION}".jar
  runtime/build/libs/quarkus-connect-"${VERSION}"-sources.jar
  deployment/build/libs/quarkus-connect-deployment-"${VERSION}".jar
  deployment/build/libs/quarkus-connect-deployment-"${VERSION}"-sources.jar
)
copied=0
for j in "${jars[@]}"; do
  if [[ -f "$j" ]]; then
    cp -v "$j" "$OUT/"
    copied=$((copied + 1))
  fi
done
# Fallback: any non-javadoc jar matching the version under those modules.
if [[ "$copied" -eq 0 ]]; then
  for j in runtime/build/libs/*"${VERSION}"*.jar deployment/build/libs/*"${VERSION}"*.jar; do
    [[ "$j" == *-javadoc.jar ]] && continue
    cp -v "$j" "$OUT/"
    copied=$((copied + 1))
  done
fi
if [[ "$copied" -eq 0 ]]; then
  echo "No jars found for version $VERSION under runtime/build/libs or deployment/build/libs" >&2
  exit 1
fi

NOTES="$OUT/RELEASE_NOTES.md"
{
  echo "# quarkus-connect $TAG"
  echo
  echo "GitHub-only artifact drop (no Maven Central). Prefer \`mavenLocal\` or source build"
  echo "until Central secrets exist — see docs/RELEASE.md."
  echo
  echo "Coordinates: \`io.github.lldata:quarkus-connect:${VERSION}\`"
  echo
  echo "## Changelog"
  echo
  # Best-effort: section matching this version, else Unreleased.
  if grep -q "^## \\[${VERSION}\\]" CHANGELOG.md; then
    awk -v ver="$VERSION" '
      $0 ~ "^## \\[" ver "\\]" {p=1; next}
      p && /^## / {exit}
      p {print}
    ' CHANGELOG.md
  else
    awk '
      /^## \[Unreleased\]/ {p=1; next}
      p && /^## / {exit}
      p {print}
    ' CHANGELOG.md
  fi
} >"$NOTES"

echo >&2
echo "Staged $copied jar(s) in $OUT" >&2
ls -la "$OUT" >&2
echo >&2
echo "Human next steps (agents never tag):" >&2
echo "  1. Ensure main has version=$VERSION and CHANGELOG heading ## [$VERSION]" >&2
echo "  2. git tag $TAG && git push origin $TAG" >&2
echo "     (publish.yml will fail closed without Central secrets — expected)" >&2
echo "  3. Create the GitHub release:" >&2
cat <<EOF
  gh release create $TAG \\
    --title "quarkus-connect $TAG" \\
    --notes-file $OUT/RELEASE_NOTES.md \\
    $OUT/*.jar
EOF
