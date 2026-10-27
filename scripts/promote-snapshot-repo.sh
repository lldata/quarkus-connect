#!/usr/bin/env bash
# Promote a Maven-local SNAPSHOT repo layout to a release version, sign it, and
# zip a Central Portal bundle. Does not compile, test, or package sources —
# input must already be a publishToMavenLocal tree for the SNAPSHOT version.
set -euo pipefail

usage() {
  echo "Usage: $0 --snapshot-repo DIR --snapshot-version VER --release-version VER --out-dir DIR" >&2
  echo "  Optional: --skip-sign (no .asc; Central will reject unsigned releases)" >&2
  exit 2
}

SNAPSHOT_REPO=""
SNAPSHOT_VERSION=""
RELEASE_VERSION=""
OUT_DIR=""
SKIP_SIGN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --snapshot-repo) SNAPSHOT_REPO="${2:-}"; shift 2 ;;
    --snapshot-version) SNAPSHOT_VERSION="${2:-}"; shift 2 ;;
    --release-version) RELEASE_VERSION="${2:-}"; shift 2 ;;
    --out-dir) OUT_DIR="${2:-}"; shift 2 ;;
    --skip-sign) SKIP_SIGN=1; shift ;;
    -h|--help) usage ;;
    *) echo "Unknown arg: $1" >&2; usage ;;
  esac
done

[[ -n "$SNAPSHOT_REPO" && -n "$SNAPSHOT_VERSION" && -n "$RELEASE_VERSION" && -n "$OUT_DIR" ]] || usage
[[ "$SNAPSHOT_VERSION" == *-SNAPSHOT ]] || {
  echo "snapshot version must end with -SNAPSHOT, got: $SNAPSHOT_VERSION" >&2
  exit 1
}
[[ "$RELEASE_VERSION" != *-SNAPSHOT ]] || {
  echo "release version must not be SNAPSHOT, got: $RELEASE_VERSION" >&2
  exit 1
}
[[ "$RELEASE_VERSION" == "${SNAPSHOT_VERSION%-SNAPSHOT}" ]] || {
  echo "release version $RELEASE_VERSION must be snapshot $SNAPSHOT_VERSION without -SNAPSHOT" >&2
  exit 1
}
[[ -d "$SNAPSHOT_REPO" ]] || {
  echo "snapshot repo not a directory: $SNAPSHOT_REPO" >&2
  exit 1
}

if [[ "$SKIP_SIGN" -eq 0 ]]; then
  command -v gpg >/dev/null || {
    echo "gpg required to sign the release bundle" >&2
    exit 1
  }
fi

GROUP_PATH="io/github/lldata"
ARTIFACTS=(quarkus-connect quarkus-connect-deployment)

STAGE="$OUT_DIR/maven-repo"
rm -rf "$STAGE"
mkdir -p "$STAGE" "$OUT_DIR/jars"

checksums() {
  local f="$1"
  # Central accepts md5/sha1; sha256/sha512 are fine extras.
  md5sum "$f" | awk '{print $1}' >"${f}.md5"
  sha1sum "$f" | awk '{print $1}' >"${f}.sha1"
  sha256sum "$f" | awk '{print $1}' >"${f}.sha256"
  sha512sum "$f" | awk '{print $1}' >"${f}.sha512"
}

sign_file() {
  local f="$1"
  [[ "$SKIP_SIGN" -eq 1 ]] && return 0
  gpg --batch --yes --detach-sign --armor -o "${f}.asc" "$f"
}

# Text entries inside jars that embed the project version and must be rewritten
# on promote. Quarkus consumers read deployment-artifact from
# META-INF/quarkus-extension.properties at build time — leaving a SNAPSHOT
# there makes a released runtime jar unusable (#63).
rewrite_jar_entry() {
  local jar="$1"
  local entry="$2"
  local tmp="$3"
  jar tf "$jar" | grep -qxF "$entry" || return 0
  mkdir -p "$tmp/$(dirname "$entry")"
  unzip -p "$jar" "$entry" \
    | sed "s/${SNAPSHOT_VERSION}/${RELEASE_VERSION}/g" \
    >"$tmp/$entry"
  jar uf "$jar" -C "$tmp" "$entry"
}

rewrite_embedded_versions() {
  local jar="$1"
  local artifact="$2"
  local tmp
  tmp="$(mktemp -d)"
  rewrite_jar_entry "$jar" "META-INF/maven/io.github.lldata/${artifact}/pom.xml" "$tmp"
  rewrite_jar_entry "$jar" "META-INF/maven/io.github.lldata/${artifact}/pom.properties" "$tmp"
  rewrite_jar_entry "$jar" "META-INF/quarkus-extension.properties" "$tmp"
  rewrite_jar_entry "$jar" "META-INF/quarkus-extension.yaml" "$tmp"
  rm -rf "$tmp"
}

# True when a jar entry name is text metadata we should scan for leftover SNAPSHOT.
is_text_jar_entry() {
  case "$1" in
    *.properties|*.xml|*.yaml|*.yml|*.MF|*.txt|*.md|*.json|*.module|*.pom|pom.xml|pom.properties)
      return 0
      ;;
    META-INF/MANIFEST.MF|META-INF/quarkus-extension.properties|META-INF/quarkus-extension.yaml)
      return 0
      ;;
  esac
  return 1
}

# Fail closed if any promoted jar or accompanying POM/module still names the
# SNAPSHOT version (covers quarkus-extension.* and anything else we missed).
assert_no_snapshot_left() {
  local root="$1"
  local found=0
  local f
  while IFS= read -r -d '' f; do
    case "$f" in
      *.jar)
        if unzip -l "$f" >/dev/null 2>&1; then
          local listing entry
          listing="$(unzip -Z1 "$f" 2>/dev/null || true)"
          while IFS= read -r entry; do
            [[ -n "$entry" && "$entry" != */ ]] || continue
            is_text_jar_entry "$entry" || continue
            if unzip -p "$f" "$entry" 2>/dev/null | grep -qF "$SNAPSHOT_VERSION"; then
              echo "SNAPSHOT version '$SNAPSHOT_VERSION' still present in $f → $entry" >&2
              found=1
            fi
          done <<<"$listing"
        fi
        ;;
      *.pom|*.module|*.xml|*.properties|*.yaml|*.yml)
        if grep -qF "$SNAPSHOT_VERSION" "$f"; then
          echo "SNAPSHOT version '$SNAPSHOT_VERSION' still present in $f" >&2
          found=1
        fi
        ;;
    esac
  done < <(find "$root" -type f \( \
    -name '*.jar' -o -name '*.pom' -o -name '*.module' \
    -o -name '*.xml' -o -name '*.properties' -o -name '*.yaml' -o -name '*.yml' \
  \) -print0)
  if [[ "$found" -ne 0 ]]; then
    echo "Promote refused: release artifacts must not mention $SNAPSHOT_VERSION" >&2
    exit 1
  fi
}

for artifact in "${ARTIFACTS[@]}"; do
  src="$SNAPSHOT_REPO/$GROUP_PATH/$artifact/$SNAPSHOT_VERSION"
  [[ -d "$src" ]] || {
    echo "missing snapshot artifact dir: $src" >&2
    exit 1
  }
  dest="$STAGE/$GROUP_PATH/$artifact/$RELEASE_VERSION"
  mkdir -p "$dest"

  shopt -s nullglob
  for f in "$src"/*; do
    base="$(basename "$f")"
    # Skip local maven metadata; release layout is the version directory only.
    case "$base" in
      _remote.repositories|*.repositories|maven-metadata-local.xml|maven-metadata-local.xml.*)
        continue
        ;;
    esac
    out_base="${base//$SNAPSHOT_VERSION/$RELEASE_VERSION}"
    cp -a "$f" "$dest/$out_base"
  done
  shopt -u nullglob

  # Rewrite versions inside POM / Gradle module metadata before checksum/sign.
  for meta in "$dest"/*.pom "$dest"/*.module; do
    [[ -f "$meta" ]] || continue
    sed -i "s/${SNAPSHOT_VERSION}/${RELEASE_VERSION}/g" "$meta"
  done

  for jar in "$dest"/*.jar; do
    [[ -f "$jar" ]] || continue
    rewrite_embedded_versions "$jar" "$artifact"
    # Stash jars for the GitHub release attachment step.
    cp -a "$jar" "$OUT_DIR/jars/"
  done

  # Drop any checksum/signature copied from the SNAPSHOT tree; regenerate.
  rm -f "$dest"/*.md5 "$dest"/*.sha1 "$dest"/*.sha256 "$dest"/*.sha512 "$dest"/*.asc

  shopt -s nullglob
  for f in "$dest"/*; do
    [[ -f "$f" ]] || continue
    case "$f" in
      *.md5|*.sha1|*.sha256|*.sha512|*.asc) continue ;;
    esac
    checksums "$f"
    sign_file "$f"
  done
  shopt -u nullglob

  echo "Promoted $artifact $SNAPSHOT_VERSION → $RELEASE_VERSION" >&2
done

assert_no_snapshot_left "$STAGE"

# Zip the Maven repo layout so archive entries start at io/...
(
  cd "$STAGE"
  zip -r -q "$OUT_DIR/central-bundle.zip" io
)

echo "central-bundle=$OUT_DIR/central-bundle.zip"
echo "jars-dir=$OUT_DIR/jars"
