# quarkus-connect

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI](https://github.com/lldata/quarkus-connect/actions/workflows/ci.yml/badge.svg)](https://github.com/lldata/quarkus-connect/actions/workflows/ci.yml)

Quarkus extension that serves ordinary `@GrpcService` beans as **gRPC + Connect + gRPC-Web** on the **Quarkus HTTP port**. Existing gRPC `ServerInterceptor`s apply to Connect and gRPC-Web.

Kotlin-first runtime with a Java-friendly SPI ([docs/SPI.md](docs/SPI.md)).

**License:** [Apache License 2.0](LICENSE) (SPDX: `Apache-2.0`).  
**Build:** Gradle.

## Status (v1)

| Capability | Support |
| --- | --- |
| `@GrpcService` → Connect unary (JSON + proto) | Yes |
| `@GrpcService` → gRPC-Web unary | Yes |
| Native gRPC on same HTTP port | Yes (`quarkus.grpc.server.use-separate-server=false`) |
| Global / method `ServerInterceptor`s on Connect | Yes |
| Server streaming (`application/connect+*`) | Yes — `@GrpcService` `SERVER_STREAMING` methods |
| Chunked media upload example | Yes — `examples/quarkus-kotlin-server` |
| Packed `google.rpc` error details | Yes |
| Client deadlines (`connect-timeout-ms`) | Yes |
| Quarkus-side Connect client | Out of scope for v1 |

## Coordinates

```kotlin
implementation("io.github.lldata:quarkus-connect:999-SNAPSHOT")
```

## Quick start

1. Depend on `quarkus-connect` + `quarkus-grpc`.
2. Implement a normal `@GrpcService` (Mutiny or blocking ImplBase).
3. Set `quarkus.grpc.server.use-separate-server=false`.

```kotlin
@GrpcService
class DemoGrpcService : MutinyDemoServiceGrpc.DemoServiceImplBase() {
    override fun echo(request: EchoRequest): Uni<EchoResponse> =
        Uni.createFrom().item(
            EchoResponse.newBuilder().setMessage(request.message).build(),
        )
}
```

```bash
curl -sS -X POST http://localhost:8080/demo.v1.DemoService/Echo \
  -H 'Content-Type: application/json' \
  -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d '{"message":"hello","clientSeq":"7"}'
```

See **`examples/quarkus-kotlin-server`** and **`examples/flutter-web-client`**.

## Content types

| Protocol | Content-Type |
| --- | --- |
| Connect unary success | `application/proto` or `application/json` |
| Connect unary error | always `application/json` |
| Connect server streaming | `application/connect+proto` or `application/connect+json` |
| gRPC-Web | `application/grpc-web+proto` (framed) |
| Native gRPC | `application/grpc` (handled by quarkus-grpc) |

## Modules

- `:quarkus-connect` (`runtime/`) — protocol + Vert.x routes + `@GrpcService` bridge
- `:quarkus-connect-deployment` (`deployment/`) — Quarkus build steps
- `:integration-tests` — `@GrpcService` + interceptor + Connect/gRPC-Web RestAssured
- `examples/` — Kotlin server + Flutter web client

Published to Maven Central (when tagged): `io.github.lldata:quarkus-connect` and
`io.github.lldata:quarkus-connect-deployment`. Local: `./gradlew publishToMavenLocal`.

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
