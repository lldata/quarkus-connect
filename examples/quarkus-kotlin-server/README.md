# examples/quarkus-kotlin-server

Quarkus Kotlin app whose only service beans are ordinary `@GrpcService` implementations.
[quarkus-connect](../../README.md) exposes them on **one HTTP port** as:

| Protocol | Content-Type | Transport |
| --- | --- | --- |
| Connect JSON (unary) | `application/json` | HTTP/1.1 or HTTP/2 |
| Connect binary (unary) | `application/proto` | HTTP/1.1 or HTTP/2 |
| Connect streaming | `application/connect+json` / `+proto` | HTTP/1.1 or HTTP/2 |
| gRPC-Web | `application/grpc-web+proto` | HTTP/1.1 or HTTP/2 |
| gRPC | `application/grpc` | HTTP/2 (Quarkus Vert.x unified server) |

`ServerInterceptor`s (here: Bearer auth) run for Connect and gRPC-Web as well as gRPC.

## Run

From the repo root:

```bash
./gradlew :examples:quarkus-kotlin-server:quarkusDev
# listening on http://localhost:8080
```

## Demo commands (Connect JSON)

```bash
# Happy path — int64 as JSON string
curl -sS -X POST http://localhost:8080/demo.v1.DemoService/Echo \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d '{"message":"hello","clientSeq":"7"}'
# {"message":"hello","clientSeq":"7","serverTimeUnixMs":"..."}

# Missing auth → UNAUTHENTICATED (HTTP 401)
curl -sS -i -X POST http://localhost:8080/demo.v1.DemoService/Echo \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -d '{"message":"hello"}'

# Guest cannot WhoAmI → PERMISSION_DENIED (HTTP 403) + trailer-retry-after-seconds
curl -sS -i -X POST http://localhost:8080/demo.v1.DemoService/WhoAmI \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-guest' \
  -d '{}'
# look for: trailer-retry-after-seconds: 30

# Invalid argument
curl -sS -i -X POST http://localhost:8080/demo.v1.DemoService/Echo \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d '{"message":""}'

# GET unary (Connect query uses connect=v1)
curl -sS 'http://localhost:8080/demo.v1.DemoService/Echo?connect=v1&encoding=json&message=%7B%22message%22%3A%22hi%22%2C%22clientSeq%22%3A%221%22%7D' \
  -H 'Authorization: Bearer demo-user'
```

Tokens: `demo-user` (full access), `demo-guest` (Echo only).

## Media upload / range / live updates

See:

- [docs/UPLOAD.md](docs/UPLOAD.md) — 100MB+ chunked unary, resume, idempotency, EDGE
- [docs/LIVE_UPDATES.md](docs/LIVE_UPDATES.md) — Connect `WatchMessages` streaming + `ListMessages` polling
- [docs/INGRESS.md](docs/INGRESS.md) — ACA / Cloudflare, CORS, gzip, OTel

```bash
# After CompleteUpload:
curl -i -H 'Range: bytes=0-1023' http://localhost:8080/media/<messageId>
curl -i http://localhost:8080/media/<messageId>/poster
```

OpenTelemetry: `RpcTracingInterceptor` records one span per RPC (procedure + status, no payloads).
