# Java-friendly SPI

The protocol layer is Kotlin-first, but registration and call handling use **Java-callable
interfaces** under `io.github.lldata.connect.spi`. A Java programming model (annotations /
codegen) can bind onto these types later without rewriting Connect framing.

## Unary

Implement `ConnectUnaryMethod<Req, Res>`:

| Method | Role |
| --- | --- |
| `procedure()` | `package.Service/Method` (no leading slash) |
| `requestType()` / `responseType()` | Protobuf message classes |
| `handle(request, context)` | Returns the response or throws `ConnectException` |

`ConnectCallContext` exposes request headers, timeout, and response header/trailer writers.
`ConnectCodec` is `PROTO` or `JSON`.

Ordinary Quarkus apps do **not** implement this SPI by hand: `@GrpcService` beans are bridged
automatically. The SPI exists for custom registrations and for a future Java DX layer.

## Server streaming

Implement `ConnectServerStreamMethod<Req, Res>` and push via `ConnectStreamObserver`:

- `onNext(value)` — zero or more messages
- `onCompleted()` or `onError(ConnectException)` — exactly one terminal signal

Wire Content-Type: `application/connect+proto` or `application/connect+json`.

## Errors

Throw `ConnectException(ConnectCode, message, details?)`. Packed `google.rpc` Any details are
carried as `ConnectErrorDetail(type, valueBase64)` and appear in Connect JSON `details[]`.
