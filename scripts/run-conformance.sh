#!/usr/bin/env bash
# Build is expected to have produced the conformance uber-jar (Gradle conformanceSuite).
# Downloads connectconformance v1.0.6 into .cache/ if not on PATH.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${CONNECTCONFORMANCE_VERSION:-1.0.6}"
CACHE="$ROOT/.cache/connectconformance"
BIN=""

if command -v connectconformance >/dev/null 2>&1; then
  BIN="$(command -v connectconformance)"
elif [[ -x "$HOME/go/bin/connectconformance" ]]; then
  BIN="$HOME/go/bin/connectconformance"
elif [[ -x "$CACHE/connectconformance" ]]; then
  BIN="$CACHE/connectconformance"
else
  mkdir -p "$CACHE"
  os="$(uname -s)"
  arch="$(uname -m)"
  case "$os-$arch" in
    Linux-x86_64) asset="connectconformance-v${VERSION}-Linux-x86_64.tar.gz" ;;
    Linux-aarch64|Linux-arm64) asset="connectconformance-v${VERSION}-Linux-aarch64.tar.gz" ;;
    Darwin-x86_64) asset="connectconformance-v${VERSION}-Darwin-x86_64.tar.gz" ;;
    Darwin-arm64) asset="connectconformance-v${VERSION}-Darwin-arm64.tar.gz" ;;
    *)
      echo "Unsupported platform $os-$arch for connectconformance download" >&2
      exit 1
      ;;
  esac
  url="https://github.com/connectrpc/conformance/releases/download/v${VERSION}/${asset}"
  echo "Downloading $url" >&2
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -fsSL "$url" -o "$tmp/cc.tgz"
  tar -xzf "$tmp/cc.tgz" -C "$tmp"
  install -m 0755 "$tmp/connectconformance" "$CACHE/connectconformance"
  BIN="$CACHE/connectconformance"
  trap - EXIT
  rm -rf "$tmp"
fi

JAR="$ROOT/conformance/build/quarkus-connect-conformance-runner.jar"
if [[ ! -f "$JAR" ]]; then
  JAR="$(ls -1 "$ROOT"/conformance/build/*-runner.jar 2>/dev/null | head -n1 || true)"
fi
if [[ -z "${JAR}" || ! -f "$JAR" ]]; then
  echo "Missing conformance uber-jar. Run: ./gradlew :conformance:quarkusBuild" >&2
  exit 1
fi

CONF="$ROOT/conformance/config/features.yaml"
KNOWN="$ROOT/conformance/config/known-failing.txt"

echo "Using connectconformance: $BIN" >&2
echo "Using server jar: $JAR" >&2

# max-servers=1 keeps Quarkus boot serial (less flake on small CI runners).
exec "$BIN" \
  --mode server \
  --conf "$CONF" \
  --known-failing "@${KNOWN}" \
  --max-servers 1 \
  -- \
  java -jar "$JAR"
