# Interop & conformance

## What runs in CI today

| Layer | Where | Covers |
| --- | --- | --- |
| RestAssured ITs | `:integration-tests` (`GreetConnectTest`) | Unary, GET, gzip, server/client/bidi streams, trailers, timeouts, auth |
| Curl matrix | `scripts/run-interop-matrix.sh` via `:examples:quarkus-kotlin-server:interopMatrix` (part of `check`) | Live example uber-jar: Echo POST/GET, auth, bad timeout, WatchMessages end-stream, GET 415 |
| Official conformance (partial) | `scripts/run-conformance.sh` via `:conformance:conformanceSuite` (part of `check`) | Thin `ConformanceService` host + `connectconformance --mode server` |

```bash
# Runs as part of ./gradlew check, or alone:
./gradlew :examples:quarkus-kotlin-server:interopMatrix
./gradlew :conformance:conformanceSuite

# Or against a manually started server:
./scripts/interop-matrix.sh http://127.0.0.1:8080
```

## Official Connect conformance (thin host)

Module `:conformance` implements [ConformanceService](https://github.com/connectrpc/conformance)
with a ServerCompat stdin/stdout adapter (`ConformanceMain`). The runner binary is taken from
`PATH`, `~/go/bin`, or downloaded into `.cache/` (v1.0.6).

### Feature matrix (`conformance/config/features.yaml`)

In scope:

- HTTP/1.1, Connect only (no gRPC / gRPC-Web / TLS / H2C)
- Codecs: proto + JSON; compression: identity + gzip
- Stream types: unary, client, server, half-duplex bidi
- Connect GET; trailers; half-duplex bidi over HTTP/1.1

Out of config (skipped entirely by the runner):

- TLS / client certs / HTTP/2 / HTTP/3 / H2C
- gRPC and gRPC-Web protocols
- Full-duplex bidi
- Message receive limits
- Brotli / zstd / deflate / snappy

### Known-failing (`conformance/config/known-failing.txt`)

Exact case names that still fail and **must** keep failing for CI to stay green.
As of unexpected-request status alignment: **364 pass / 0 known-failing** on the thin matrix.

Remove entries from `known-failing.txt` when a case starts passing — the runner requires known-failing cases to fail.

## Streaming cancel / timeout notes

- `connect-timeout-ms` must be a positive integer ≤ 10 digits (invalid → `invalid_argument`)
- Server/bidi streams attach a `StreamCallControl`: HTTP response close/exception cancels the gRPC `Context`
- Client/bidi still buffer the request body (vertical slice); interactive duplex is a later hardening item
