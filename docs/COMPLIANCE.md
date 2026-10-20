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
| gRPC-Web unary | **Yes** | `+proto` (and `+json` codec path) |
| Native gRPC on same port | **Yes** | `quarkus.grpc.server.use-separate-server=false` |
| Interceptors on Connect | **Yes** | `@GlobalInterceptor` via bridge |
| Quarkus Connect *client* | **Out of scope** | |

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
- Full [connectrpc/conformance](https://github.com/connectrpc/conformance) matrix (TLS, gRPC, H2, …) —
  thin host + partial suite runs in CI today (see [INTEROP.md](INTEROP.md)); curl matrix + ITs remain

- Maven Central secrets + first tagged release (`docs/RELEASE.md`)
- Server-side ffmpeg thumbnails
