#!/usr/bin/env bash
# Start the example uber-jar (built by Gradle interopMatrix → quarkusBuild),
# run scripts/interop-matrix.sh, tear down.
# Do not nest ./gradlew here: a nested build rewrites :quarkus-connect:jar while
# parallel :integration-tests:test may still be reading it (CI Zip EOF flake).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="$(sed -n 's/^version=//p' gradle.properties)"
JAR="$ROOT/examples/quarkus-kotlin-server/build/quarkus-kotlin-server-${VERSION}-runner.jar"
if [[ ! -f "$JAR" ]]; then
  JAR="$(ls -1 "$ROOT"/examples/quarkus-kotlin-server/build/*-runner.jar 2>/dev/null | head -n1 || true)"
fi
if [[ -z "${JAR}" || ! -f "$JAR" ]]; then
  echo "Missing example uber-jar. Run: ./gradlew :examples:quarkus-kotlin-server:quarkusBuild" >&2
  exit 1
fi
LOG="$ROOT/examples/quarkus-kotlin-server/build/interop-server.log"
mkdir -p "$(dirname "$LOG")"

java -jar "$JAR" >"$LOG" 2>&1 &
PID=$!
cleanup() { kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; }
trap cleanup EXIT

BASE=http://127.0.0.1:8080
for i in $(seq 1 60); do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "server died (jar=$JAR):"
    tail -n 80 "$LOG" || true
    exit 1
  fi
  code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/demo.v1.DemoService/Echo" \
    -H 'Content-Type: application/json' \
    -H 'connect-protocol-version: 1' \
    -H 'Authorization: Bearer demo-user' \
    -d '{"message":"ready","clientSeq":"0"}' 2>/dev/null || echo 000)
  if [[ "$code" == "200" ]]; then
    break
  fi
  if [[ "$i" -eq 60 ]]; then
    echo "server not ready:"
    tail -n 80 "$LOG" || true
    exit 1
  fi
  sleep 1
done

exec "$ROOT/scripts/interop-matrix.sh" "$BASE"
