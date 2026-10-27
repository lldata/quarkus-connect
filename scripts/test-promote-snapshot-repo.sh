#!/usr/bin/env bash
# Smoke-test promote-snapshot-repo.sh: rewrite quarkus-extension.* and refuse
# leftover SNAPSHOT coordinates (#63). No Gradle, no network, no GPG.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROMOTE="$ROOT/scripts/promote-snapshot-repo.sh"
SNAP_VER="9.9.9-SNAPSHOT"
REL_VER="9.9.9"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

make_artifact() {
  local artifact="$1"
  local dir="$WORK/snap/io/github/lldata/${artifact}/${SNAP_VER}"
  mkdir -p "$dir" "$WORK/jarbuild/META-INF/maven/io.github.lldata/${artifact}"

  cat >"$WORK/jarbuild/META-INF/maven/io.github.lldata/${artifact}/pom.properties" <<EOF
version=${SNAP_VER}
groupId=io.github.lldata
artifactId=${artifact}
EOF
  cat >"$WORK/jarbuild/META-INF/maven/io.github.lldata/${artifact}/pom.xml" <<EOF
<project><version>${SNAP_VER}</version></project>
EOF

  if [[ "$artifact" == "quarkus-connect" ]]; then
    # Quarkus writes escaped colons in the properties value.
    printf '%s\n' \
      "deployment-artifact=io.github.lldata\\:quarkus-connect-deployment\\:${SNAP_VER}" \
      >"$WORK/jarbuild/META-INF/quarkus-extension.properties"
    cat >"$WORK/jarbuild/META-INF/quarkus-extension.yaml" <<EOF
name: "Connect RPC"
artifact: "io.github.lldata:quarkus-connect::jar:${SNAP_VER}"
EOF
  fi

  (
    cd "$WORK/jarbuild"
    jar cf "$dir/${artifact}-${SNAP_VER}.jar" META-INF
  )
  cat >"$dir/${artifact}-${SNAP_VER}.pom" <<EOF
<?xml version="1.0"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.github.lldata</groupId>
  <artifactId>${artifact}</artifactId>
  <version>${SNAP_VER}</version>
</project>
EOF
  rm -rf "$WORK/jarbuild"
}

make_artifact quarkus-connect
make_artifact quarkus-connect-deployment

out="$WORK/out"
"$PROMOTE" \
  --snapshot-repo "$WORK/snap" \
  --snapshot-version "$SNAP_VER" \
  --release-version "$REL_VER" \
  --out-dir "$out" \
  --skip-sign

runtime_jar="$out/jars/quarkus-connect-${REL_VER}.jar"
props="$(unzip -p "$runtime_jar" META-INF/quarkus-extension.properties)"
yaml="$(unzip -p "$runtime_jar" META-INF/quarkus-extension.yaml)"
embedded_pom="$(unzip -p "$runtime_jar" META-INF/maven/io.github.lldata/quarkus-connect/pom.properties)"

printf '%s\n' "$props" | grep -qF "$SNAP_VER" \
  && { echo "FAIL: SNAPSHOT still in quarkus-extension.properties:" >&2; echo "$props" >&2; exit 1; }
printf '%s\n' "$props" | grep -qF "quarkus-connect-deployment\\:${REL_VER}" \
  || { echo "FAIL: deployment-artifact not rewritten to $REL_VER:" >&2; echo "$props" >&2; exit 1; }
printf '%s\n' "$yaml" | grep -qF "$SNAP_VER" \
  && { echo "FAIL: SNAPSHOT still in quarkus-extension.yaml:" >&2; echo "$yaml" >&2; exit 1; }
printf '%s\n' "$yaml" | grep -qF ":jar:${REL_VER}" \
  || { echo "FAIL: yaml artifact not rewritten to $REL_VER:" >&2; echo "$yaml" >&2; exit 1; }
printf '%s\n' "$embedded_pom" | grep -qF "version=${REL_VER}" \
  || { echo "FAIL: embedded pom.properties version not rewritten:" >&2; echo "$embedded_pom" >&2; exit 1; }

# Refuse path: drop rewrite of quarkus-extension.properties and confirm promote exits non-zero.
broken_promote="$WORK/broken-promote.sh"
# Remove only the rewrite_jar_entry call for quarkus-extension.properties.
sed '/rewrite_jar_entry .*META-INF\/quarkus-extension\.properties/d' \
  "$PROMOTE" >"$broken_promote"
chmod +x "$broken_promote"
broken_out="$WORK/broken-out"
set +e
"$broken_promote" \
  --snapshot-repo "$WORK/snap" \
  --snapshot-version "$SNAP_VER" \
  --release-version "$REL_VER" \
  --out-dir "$broken_out" \
  --skip-sign >"$WORK/broken.out" 2>"$WORK/broken.err"
rc=$?
set -e
[[ "$rc" -ne 0 ]] || {
  echo "FAIL: promote without extension-properties rewrite should have refused" >&2
  exit 1
}
grep -qF "$SNAP_VER" "$WORK/broken.err" \
  || { echo "FAIL: refusal did not mention SNAPSHOT version:" >&2; cat "$WORK/broken.err" >&2; exit 1; }
grep -q "quarkus-extension.properties" "$WORK/broken.err" \
  || { echo "FAIL: refusal did not name quarkus-extension.properties:" >&2; cat "$WORK/broken.err" >&2; exit 1; }

echo "OK: promote rewrites quarkus-extension.* and refuses leftover SNAPSHOT"
