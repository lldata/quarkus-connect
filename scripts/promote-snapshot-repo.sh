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

rewrite_embedded_pom() {
  local jar="$1"
  local artifact="$2"
  local tmp
  tmp="$(mktemp -d)"
  # Best-effort: update META-INF/maven pom so coordinates match the release.
  if jar tf "$jar" | grep -q "META-INF/maven/io.github.lldata/${artifact}/pom.xml"; then
    mkdir -p "$tmp/META-INF/maven/io.github.lldata/${artifact}"
    unzip -p "$jar" "META-INF/maven/io.github.lldata/${artifact}/pom.xml" \
      | sed "s/${SNAPSHOT_VERSION}/${RELEASE_VERSION}/g" \
      >"$tmp/META-INF/maven/io.github.lldata/${artifact}/pom.xml"
    unzip -p "$jar" "META-INF/maven/io.github.lldata/${artifact}/pom.properties" \
      | sed "s/${SNAPSHOT_VERSION}/${RELEASE_VERSION}/g" \
      >"$tmp/META-INF/maven/io.github.lldata/${artifact}/pom.properties"
    jar uf "$jar" -C "$tmp" "META-INF/maven/io.github.lldata/${artifact}/pom.xml"
    jar uf "$jar" -C "$tmp" "META-INF/maven/io.github.lldata/${artifact}/pom.properties"
  fi
  rm -rf "$tmp"
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
    rewrite_embedded_pom "$jar" "$artifact"
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

# Zip the Maven repo layout so archive entries start at io/...
(
  cd "$STAGE"
  zip -r -q "$OUT_DIR/central-bundle.zip" io
)

echo "central-bundle=$OUT_DIR/central-bundle.zip"
echo "jars-dir=$OUT_DIR/jars"
