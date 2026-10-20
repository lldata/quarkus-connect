#!/usr/bin/env bash
# Thin Connect interop matrix against a running quarkus-connect example server.
# Official connectrpc/conformance (ConformanceService) is a follow-up — see docs/INTEROP.md.
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
AUTH_HEADER="${AUTH_HEADER:-Authorization: Bearer demo-user}"
PASS=0
FAIL=0

green() { printf '\033[32m✓\033[0m %s\n' "$*"; }
red() { printf '\033[31m✗\033[0m %s\n' "$*"; }

assert_contains() {
  local name="$1" haystack="$2" needle="$3"
  if [[ "$haystack" == *"$needle"* ]]; then
    green "$name"
    PASS=$((PASS + 1))
  else
    red "$name (expected to contain: $needle)"
    printf '  body: %s\n' "$(printf '%s' "$haystack" | head -c 400)"
    FAIL=$((FAIL + 1))
  fi
}

assert_http() {
  local name="$1" code="$2" expected="$3"
  if [[ "$code" == "$expected" ]]; then
    green "$name (HTTP $code)"
    PASS=$((PASS + 1))
  else
    red "$name (HTTP $code, expected $expected)"
    FAIL=$((FAIL + 1))
  fi
}

# Connect streaming envelope → file (avoid bash NUL issues)
write_connect_envelope() {
  local out="$1" payload="$2"
  python3 - "$out" "$payload" <<'PY'
import pathlib, sys
out, payload = sys.argv[1], sys.argv[2].encode("utf-8")
pathlib.Path(out).write_bytes(bytes([0]) + len(payload).to_bytes(4, "big") + payload)
PY
}

echo "== Connect interop matrix → $BASE_URL =="

# 1. Unary JSON POST
body=$(curl -sS -o /tmp/qc-unary.json -w '%{http_code}' -X POST "$BASE_URL/demo.v1.DemoService/Echo" \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Accept-Encoding: identity' \
  -H "$AUTH_HEADER" \
  -d '{"message":"interop","clientSeq":"1"}')
assert_http "unary JSON POST" "$body" "200"
assert_contains "unary JSON body" "$(cat /tmp/qc-unary.json)" '"message"'

# 2. Unary GET
body=$(curl -sS -o /tmp/qc-get.json -w '%{http_code}' \
  "$BASE_URL/demo.v1.DemoService/Echo?connect=v1&encoding=json&message=%7B%22message%22%3A%22get%22%2C%22clientSeq%22%3A%222%22%7D" \
  -H 'Accept-Encoding: identity' \
  -H "$AUTH_HEADER")
assert_http "unary GET" "$body" "200"
assert_contains "unary GET body" "$(cat /tmp/qc-get.json)" 'get'

# 3. Auth failure → unauthenticated
body=$(curl -sS -o /tmp/qc-unauth.json -w '%{http_code}' -X POST "$BASE_URL/demo.v1.DemoService/Echo" \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Accept-Encoding: identity' \
  -d '{"message":"x","clientSeq":"1"}')
assert_http "missing bearer" "$body" "401"
assert_contains "unauthenticated code" "$(cat /tmp/qc-unauth.json)" 'unauthenticated'

# 4. Invalid connect-timeout-ms
body=$(curl -sS -o /tmp/qc-timeout.json -w '%{http_code}' -X POST "$BASE_URL/demo.v1.DemoService/Echo" \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'connect-timeout-ms: abc' \
  -H 'Accept-Encoding: identity' \
  -H "$AUTH_HEADER" \
  -d '{"message":"x","clientSeq":"1"}')
assert_http "bad timeout" "$body" "400"
assert_contains "invalid_argument timeout" "$(cat /tmp/qc-timeout.json)" 'invalid_argument'

# 5. Server streaming WatchMessages (catch-up may be empty; must end-stream)
write_connect_envelope /tmp/qc-stream-req.bin '{"afterSequence":"0"}'
code=$(curl -sS -o /tmp/qc-stream.bin -w '%{http_code}' -X POST "$BASE_URL/media.v1.MediaService/WatchMessages" \
  -H 'Content-Type: application/connect+json' \
  -H 'connect-protocol-version: 1' \
  -H 'connect-accept-encoding: identity' \
  -H "$AUTH_HEADER" \
  --data-binary @/tmp/qc-stream-req.bin)
assert_http "server stream WatchMessages" "$code" "200"
# End-stream flag bit1 (0x02) somewhere in the body
if python3 - <<'PY'
import pathlib, sys
raw = pathlib.Path("/tmp/qc-stream.bin").read_bytes()
off = 0
saw_end = False
while off + 5 <= len(raw):
    flag = raw[off]
    ln = int.from_bytes(raw[off+1:off+5], "big")
    if flag & 0x02:
        saw_end = True
    off += 5 + ln
sys.exit(0 if saw_end else 1)
PY
then
  green "server stream end-stream present"
  PASS=$((PASS + 1))
else
  red "server stream missing end-stream"
  FAIL=$((FAIL + 1))
fi

# 6. Unsupported encoding still 415
body=$(curl -sS -o /tmp/qc-415.json -w '%{http_code}' \
  "$BASE_URL/demo.v1.DemoService/Echo?connect=v1&encoding=xml&message=%7B%7D" \
  -H "$AUTH_HEADER")
assert_http "GET encoding 415" "$body" "415"

echo
echo "Results: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
