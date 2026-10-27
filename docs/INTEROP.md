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

- HTTP/1.1 + **HTTP/2 cleartext (h2c)**
- **Connect**, **gRPC-Web**, and **native gRPC** (`PROTOCOL_GRPC`, proto codec) on H2
- Codecs: proto + JSON (JSON for Connect + gRPC-Web only; gRPC×JSON excluded)
- Compression: identity + gzip (including gRPC-Web compressed frames)
- Stream types: unary, client, server, half-duplex bidi
- Connect GET; trailers; half-duplex bidi over HTTP/1.1

Out of config / excluded:

- TLS / client certs / HTTP/3
- Native gRPC × JSON (`exclude_cases`)
- Full-duplex bidi (client/bidi still buffer the request body until streaming lands)
- Brotli / zstd / deflate / snappy

Message receive limits: `supportsMessageReceiveLimit: true`; the host sets
`quarkus.connect.max-inbound-message-size` from `message_receive_limit`.

gRPC-Web on h2c is claimed by quarkus-connect at a route order before quarkus-grpc.
quarkus-grpc's `isGrpc` matches `^application/grpc.*` on HTTP/2, which would otherwise
steal `application/grpc-web*` and answer with native `application/grpc`.

### Known-failing / known-flaky

- `known-failing.txt` — must fail (quarkus-grpc unexpected-request mapping for native gRPC)
- `known-flaky.txt` — may pass or fail (Vert.x padded `grpc-status-details-bin` on
  Errors/Duplicate Metadata for `PROTOCOL_GRPC`)

As of native gRPC enablement: **~1514 pass** with 10 known-failing + flaky padding suite.
See comments in the config files.

Remove a known-failing entry when it starts passing — the runner requires those cases to fail.

## Streaming cancel / timeout notes

- `connect-timeout-ms` must be a positive integer ≤ 10 digits (invalid → `invalid_argument`)
- Server/bidi streams attach a `StreamCallControl`: HTTP response close/exception cancels the gRPC `Context`
- Client/bidi still buffer the request body (vertical slice; tracked in #43); interactive duplex is a later hardening item
