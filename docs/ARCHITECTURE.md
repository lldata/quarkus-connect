# Architecture — quarkus-connect v1

## Modules

| Project | Dir | Role |
| --- | --- | --- |
| `:quarkus-connect` | `runtime/` | Protocol + Vert.x routes + `@GrpcService` bridge |
| `:quarkus-connect-deployment` | `deployment/` | Build-time processor (feature `connect`) |
| `:integration-tests` | `integration-tests/` | RestAssured coverage |
| `:examples:quarkus-kotlin-server` | `examples/…` | Demo server + media upload |

Build: JDK **21** toolchain, library bytecode **JVM 17**. `./gradlew check`.

## Layers

1. **SPI** (`io.github.lldata.connect.spi`) — `ConnectUnaryMethod`, `ConnectServerStreamMethod`,
   `ConnectCode`, `ConnectException`, `ConnectCodec`, `ConnectCallContext`. Java-callable; see
   [SPI.md](SPI.md).
2. **Protocol** — unary encode/decode, Connect envelopes, error JSON, header constants.
3. **Runtime** — CDI registry + Vert.x handler (Connect unary / Connect streaming / gRPC-Web).
4. **gRPC bridge** — discovers `@GrpcService` + `@GlobalInterceptor`s; unary and server-streaming.

## One port, three protocols

| Protocol | Content-Type | Handler |
| --- | --- | --- |
| Connect unary | `application/json` / `application/proto` | quarkus-connect |
| Connect server streaming | `application/connect+json` / `+proto` | quarkus-connect |
| gRPC-Web unary | `application/grpc-web+proto` | quarkus-connect |
| Native gRPC | `application/grpc` | quarkus-grpc (`use-separate-server=false`) |

## Unary wire (Connect)

| | |
| --- | --- |
| Path | `/package.Service/Method` |
| Success CT | same as request (`application/proto` or `application/json`) |
| Error CT | always `application/json` + non-200 from `ConnectCode` |
| Trailers | response headers prefixed `trailer-` |
| GET | `?connect=v1&encoding=…&message=…` (`base64=1` for proto) |
| Version header | `connect-protocol-version: 1` (POST) |

## Programming model

Preferred: ordinary Quarkus `@GrpcService` beans. Low-level CDI beans implementing
`ConnectUnaryMethod` / `ConnectServerStreamMethod` remain supported (see integration-tests
`PingConnectMethod`).

Client-streaming and bidi `@GrpcService` methods are discovered but **not** bridged to Connect;
Connect/gRPC-Web requests for those procedures return `unimplemented` with the gRPC method type
named in the message. Native `application/grpc` still reaches quarkus-grpc.
