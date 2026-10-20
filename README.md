# quarkus-connect

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI](https://github.com/lldata/quarkus-connect/actions/workflows/ci.yml/badge.svg)](https://github.com/lldata/quarkus-connect/actions/workflows/ci.yml)

Quarkus extension that implements the [Connect RPC](https://connectrpc.com/) **server** protocol over Vert.x HTTP.

Kotlin-first runtime with a Java-friendly SPI so a Java programming model can be added later without rewriting the protocol layer.

**License:** [Apache License 2.0](LICENSE) (SPDX: `Apache-2.0`).  
**Build:** Gradle (house choice for this extension).

## Status (v1)

| Capability | Support |
| --- | --- |
| Unary Connect (POST) | Yes — `application/proto`, `application/json` |
| Unary Connect (GET) | Yes — query `encoding`, `message`, `connect=1` |
| Streaming (`application/connect+*`) | Not yet |
| gRPC / gRPC-Web wire protocols | Not yet (Connect clients only) |
| Quarkus-side Connect client | Out of scope for v1 |

Target clients include any Connect-compliant stack (e.g. connect-es, connect-go, Flutter web via Connect).

## Coordinates

```kotlin
implementation("io.github.lldata:quarkus-connect:999-SNAPSHOT")
```

```xml
<dependency>
  <groupId>io.github.lldata</groupId>
  <artifactId>quarkus-connect</artifactId>
  <version>999-SNAPSHOT</version>
</dependency>
```

Group `io.github.lldata` (greenfield).

## Quick start

Implement `ConnectUnaryMethod` (protobuf request/response) and register it as a CDI bean:

```kotlin
@ApplicationScoped
class GreetConnectService : ConnectUnaryMethod<GreetRequest, GreetResponse> {
    override fun procedure() = "greet.v1.GreetService/Greet"
    override fun requestType() = GreetRequest::class.java
    override fun responseType() = GreetResponse::class.java

    override fun handle(request: GreetRequest, context: ConnectCallContext): GreetResponse =
        GreetResponse.newBuilder().setGreeting("Hello, ${request.name}!").build()
}
```

Call it with a Connect client or curl:

```bash
curl -sS -X POST http://localhost:8080/greet.v1.GreetService/Greet \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -d '{"name":"Buf"}'
# {"greeting":"Hello, Buf!"}
```

## Content types

| RPC shape | Content-Type | Body |
| --- | --- | --- |
| Unary success | `application/proto` or `application/json` | Bare protobuf / JSON message (no gRPC framing) |
| Unary error | always `application/json` | `{"code":"…","message":"…"}` |
| Streaming (future) | `application/connect+proto` / `+json` | Envelope-framed |

Optional request header: `connect-protocol-version: 1`. Optional timeout: `connect-timeout-ms`. Trailing metadata is returned as ordinary headers prefixed with `trailer-`.

## Error mapping (unary)

Connect codes map to HTTP statuses ([protocol reference](https://connectrpc.com/docs/protocol/#error-codes)):

| Connect code | HTTP |
| --- | --- |
| `invalid_argument` / `failed_precondition` / `out_of_range` | 400 |
| `unauthenticated` | 401 |
| `permission_denied` | 403 |
| `not_found` | 404 |
| `aborted` / `already_exists` | 409 |
| `resource_exhausted` | 429 |
| `canceled` | 499 |
| `unimplemented` | 501 |
| `unavailable` | 503 |
| `deadline_exceeded` | 504 |
| `unknown` / `internal` / `data_loss` | 500 |

Unsupported `Content-Type` → HTTP **415** with a Connect JSON error body.

## Modules

- `:quarkus-connect` (dir `runtime/`) — protocol + Vert.x route + SPI
- `:quarkus-connect-deployment` (dir `deployment/`) — Quarkus build steps
- `:integration-tests` — sample `greet.v1.GreetService` + RestAssured checks

## Build

**JDK:** build on **21** toolchain; library bytecode targets **JVM 17** (`jdkToolchainVersion` /
`jvmTargetVersion` in `gradle.properties`, `.java-version`, Foojay resolver). CI installs Temurin
21 then runs Gradle — same split as
[lldata/quarkus-kotlin-typescript-rest-client-generator](https://github.com/lldata/quarkus-kotlin-typescript-rest-client-generator).

```bash
./gradlew check
```

## License

[Apache License 2.0](LICENSE) — SPDX `Apache-2.0`.

## Maintenance

See [AGENTS.md](AGENTS.md) for agent-maintainer rules. CI, Dependabot, CODEOWNERS, and the
tag-gated publish workflow follow house style from
[lldata/quarkus-kotlin-typescript-rest-client-generator](https://github.com/lldata/quarkus-kotlin-typescript-rest-client-generator).
