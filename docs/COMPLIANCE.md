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
| Content-Encoding / compression | **identity only** | Unary `Content-Encoding`, GET `compression=`, stream `connect-content-encoding` → `unimplemented` if not identity (Quarkus may gzip HTTP responses separately) |
| `Accept-Encoding: identity` advertised | **Yes** | Unary responses; streaming uses `connect-accept-encoding: identity` |
| Server streaming `application/connect+*` | **Yes** | End-stream envelope on completion/error; unsupported stream encoding → end-stream `unimplemented` |
| Client / bidi streaming over Connect | **No** | Clear `unimplemented`; native gRPC still serves them |
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

- Connect client & bidi streaming
- Request-body gzip/br for Connect
- Maven Central secrets + first tagged release (`docs/RELEASE.md`)
- Server-side ffmpeg thumbnails
