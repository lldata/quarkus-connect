# Interop & conformance

## What runs in CI today

| Layer | Where | Covers |
| --- | --- | --- |
| RestAssured ITs | `:integration-tests` (`GreetConnectTest`) | Unary, GET, gzip, server/client/bidi streams, trailers, timeouts, auth |
| Curl matrix | `scripts/run-interop-matrix.sh` via `:examples:quarkus-kotlin-server:interopMatrix` (part of `check`) | Live example uber-jar: Echo POST/GET, auth, bad timeout, WatchMessages end-stream, GET 415 |

```bash
# Runs as part of ./gradlew check, or alone:
./gradlew :examples:quarkus-kotlin-server:interopMatrix

# Or against a manually started server:
./scripts/interop-matrix.sh http://127.0.0.1:8080
```

## Official Connect conformance (follow-up)

The [connectrpc/conformance](https://github.com/connectrpc/conformance) runner expects a
**ConformanceService** implementation (stdin/stdout process + feature config YAML). That is a
substantial proto + host surface beyond ordinary `@GrpcService` demos.

Tracked follow-ups (not blocked on secrets):

1. Scaffold a `conformance/` module implementing ConformanceService on quarkus-connect
2. Wire `connectconformance --mode server` in CI (Go install of the runner)
3. Record known-failing cases (e.g. interactive half-close without full body buffer) until chunked streaming lands

Until then, the curl matrix + ITs are the practical interop gate.

## Streaming cancel / timeout notes

- `connect-timeout-ms` must be a positive integer ≤ 10 digits (invalid → `invalid_argument`)
- Server/bidi streams attach a `StreamCallControl`: HTTP response close/exception cancels the gRPC `Context`
- Client/bidi still buffer the request body (vertical slice); interactive duplex is a later hardening item
