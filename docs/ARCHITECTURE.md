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
4. **gRPC bridge** — discovers `@GrpcService` + `@GlobalInterceptor`s; unary + all streaming kinds.

## One port, three protocols

| Protocol | Content-Type | Handler |
| --- | --- | --- |
| Connect unary | `application/json` / `application/proto` | quarkus-connect |
| Connect server / client / bidi streaming | `application/connect+json` / `+proto` | quarkus-connect |
| gRPC-Web unary / streaming | `application/grpc-web+proto` (etc.) | quarkus-connect (route order before quarkus-grpc on h2) |
| Native gRPC | `application/grpc` | quarkus-grpc (`use-separate-server=false`) |

## Unary wire (Connect)

| | |
| --- | --- |
| Path | `/package.Service/Method` |
| Success CT | same as request (`application/proto` or `application/json`) |
| Error CT | always `application/json` + non-200 from `ConnectCode` |
| Trailers | response headers prefixed `trailer-` |
| GET | `?connect=v1&encoding=…&message=…` (`base64=1` for proto; `compression=gzip` needs base64) |
| Version header | `connect-protocol-version: 1` (POST) |
| Compression | `identity` + `gzip` (unary whole-body; streaming per-envelope) |

## Programming model

Preferred: ordinary Quarkus `@GrpcService` beans. Low-level CDI beans implementing
`ConnectUnaryMethod` / `ConnectServerStreamMethod` / `ConnectClientStreamMethod` /
`ConnectBidiStreamMethod` remain supported (see integration-tests `PingConnectMethod`).

Client/bidi Connect handlers buffer the HTTP request body for the vertical slice (fine for
demos; large or interactive streams can move to chunked reads later). Native `application/grpc`
still reaches quarkus-grpc.

## Clients (not this extension)

**quarkus-connect is the server.** JVM / Quarkus apps that need a Connect **client** should
use the official **Quarkus Connect client** path: ConnectRPC’s
[Connect-Kotlin](https://connectrpc.com/docs/kotlin/getting-started/)
(`com.connectrpc:connect-kotlin`, OkHttp transport `com.connectrpc:connect-kotlin-okhttp`).
There is no separate Quarkus-catalog Connect-client extension to prefer over that.

Non-JVM examples in this repo: Flutter (`examples/flutter-web-client`); other ecosystems use
[connect-es](https://connectrpc.com/docs/web/getting-started/) and siblings. Do not treat
“no JVM client in this repository” as a product gap.
