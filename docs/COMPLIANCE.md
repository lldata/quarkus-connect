# Connect protocol compliance (v1)

Honest matrix against the [Connect protocol reference](https://connectrpc.com/docs/protocol/).
Examples requirements live in the Project store (`examples-requirements.md`); this file is
in-repo so external users see the same floor.

| Area | Status | Notes |
| --- | --- | --- |
| Unary POST `application/json` / `proto` | **Yes** | Same `@GrpcService` as native gRPC |
| Unary GET `connect=v1` | **Yes** | `encoding`, `message`, optional `base64=1`; unknown encoding → **415** |
| Unary errors JSON + HTTP mapping | **Yes** | Empty `message` omitted; `details[]` for packed `google.rpc` |
| Unary trailers as `trailer-*` | **Yes** | |
| `connect-protocol-version: 1` | **Yes** | Unsupported → `invalid_argument` |
| `connect-timeout-ms` / deadlines | **Yes** | |
| Content-Encoding / compression | **identity + gzip** | Unary `Content-Encoding` / GET `compression=` / stream `connect-content-encoding`; response negotiation via `Accept-Encoding` / `connect-accept-encoding`. Other codings (br, zstd, …) → `unimplemented` |
| Accept-Encoding advertisement | **Yes** | Unary `Accept-Encoding: gzip, identity`; streaming `connect-accept-encoding: gzip, identity` |
| Server streaming `application/connect+*` | **Yes** | End-stream envelope on completion/error; unsupported stream encoding → end-stream `unimplemented` |
| Client streaming over Connect | **Yes** | `@GrpcService` `CLIENT_STREAMING`; buffered request body (vertical slice) |
| Bidi streaming over Connect | **Yes** | `@GrpcService` `BIDI_STREAMING`; buffered request body (vertical slice) |
| gRPC-Web unary + streaming | **Yes** | `+proto` / `+json`; gzip frames; H1 + H2 h2c conformance green |
| HTTP/2 cleartext (h2c) | **Yes** | Connect + gRPC-Web + native gRPC (proto) in conformance |
| Native gRPC on same port | **Yes** | `quarkus.grpc.server.use-separate-server=false`; conformance `PROTOCOL_GRPC` (proto; JSON excluded). Error-detail trailers still padded by Vert.x `Utils.writeMetadata` (known-flaky) |
| Interceptors on Connect | **Yes** | `@GlobalInterceptor` via bridge |
| Quarkus Security on Connect | **Opt-in** | App adds `quarkus-security` + mechanism; identity propagation + `@RolesAllowed` / `@Authenticated` on Connect/gRPC-Web (`:integration-tests-security`) |
| JVM Connect *client* | **Official, external** | This repo is **server-only**. Use [Connect-Kotlin](https://connectrpc.com/docs/kotlin/getting-started/) (`com.connectrpc:connect-kotlin`, transport `com.connectrpc:connect-kotlin-okhttp`) — the Quarkus Connect client for JVM/Kotlin. Not a gap to build here. |

## Examples proof (summary)

| Requirement | Status |
| --- | --- |
| One port / three protocols / interceptors | Met |
| Flutter web (+ IO conditional import for mobile) | Met |
| Auth + status mapping + deadlines | Met |
| Chunked upload / resume / range / poster | Met (EDGE soak manual) |
| Live updates streaming + reconnect | Met |
| OTel + CORS/gzip ingress notes | Met |

## Deliberately later / human

- Streaming request body without full buffering (large client/bidi streams; interactive half-close)
- **Brotli (`br`) / zstd** — deferred: JDK has no brotli API; portable codecs need native libs
  (e.g. brotli4j + platform classifiers), which is a poor fit for a Central-published extension.
  Prefer edge/Quarkus HTTP `br` for whole responses; Connect message coding stays `gzip` + `identity`.
- Full [connectrpc/conformance](https://github.com/connectrpc/conformance) matrix (TLS, HTTP/3,
  gRPC×JSON, full-duplex, message limits) — thin host + partial suite in CI (see [INTEROP.md](INTEROP.md))
- Unpadded `grpc-status-details-bin` on the **native** gRPC path (Vert.x pads Base64; Connect /
  gRPC-Web paths already emit unpadded)
- Maven Central secrets (`docs/RELEASE.md` Path B)
- Server-side ffmpeg thumbnails
