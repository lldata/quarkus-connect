# examples/quarkus-kotlin-server

Quarkus Kotlin app whose only service beans are ordinary `@GrpcService` implementations.
[quarkus-connect](../../README.md) exposes them on **one HTTP port** as:

| Protocol | Content-Type | Transport |
| --- | --- | --- |
| Connect JSON | `application/json` | HTTP/1.1 or HTTP/2 |
| Connect binary | `application/proto` | HTTP/1.1 or HTTP/2 |
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

# Guest cannot WhoAmI → PERMISSION_DENIED (HTTP 403)
curl -sS -i -X POST http://localhost:8080/demo.v1.DemoService/WhoAmI \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-guest' \
  -d '{}'

# Invalid argument
curl -sS -i -X POST http://localhost:8080/demo.v1.DemoService/Echo \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d '{"message":""}'
```

Tokens: `demo-user` (full access), `demo-guest` (Echo only).
