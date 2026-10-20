# Java-friendly SPI

The protocol layer is Kotlin-first, but registration and call handling use **Java-callable
interfaces** under `io.github.lldata.connect.spi`. A Java programming model (annotations /
codegen) can bind onto these types later without rewriting Connect framing.

Ordinary Quarkus apps should prefer `@GrpcService` (bridged automatically). Use the SPI when you
need a Connect-only procedure or to prove a Java programming model.

## Unary

Implement `ConnectUnaryMethod<Req, Res>` as an `@ApplicationScoped` CDI bean:

| Method | Role |
| --- | --- |
| `procedure()` | `package.Service/Method` (no leading slash) |
| `requestType()` / `responseType()` | Protobuf message classes |
| `handle(request, context)` | Returns the response or throws `ConnectException` |

`ConnectCallContext` exposes request headers, timeout, and response header/trailer writers.
Trailers are written on the wire as `trailer-<name>` (Connect unary). `ConnectCodec` is `PROTO`
or `JSON`.

### Java example

```java
@ApplicationScoped
public class PingConnectMethod implements ConnectUnaryMethod<GreetRequest, GreetResponse> {
  @Override public String procedure() { return "demo.v1.DemoService/Ping"; }
  @Override public Class<GreetRequest> requestType() { return GreetRequest.class; }
  @Override public Class<GreetResponse> responseType() { return GreetResponse.class; }

  @Override
  public GreetResponse handle(GreetRequest request, ConnectCallContext context) {
    context.responseHeader("x-ping", "1");
    context.responseTrailer("x-ping-trail", "ok"); // → HTTP header trailer-x-ping-trail
    return GreetResponse.newBuilder().setGreeting("Pong").build();
  }
}
```

See `integration-tests/.../PingConnectMethod.java` for a working bean.

## Server streaming

Implement `ConnectServerStreamMethod<Req, Res>` and push via `ConnectStreamObserver`:

- `onNext(value)` — zero or more messages
- `onCompleted()` or `onError(ConnectException)` — exactly one terminal signal

Wire Content-Type: `application/connect+proto` or `application/connect+json`.

## Client streaming

Implement `ConnectClientStreamMethod<Req, Res>`:

| Method | Role |
| --- | --- |
| `handle(requests, context)` | Receives the full list of request messages; returns one response |

Same `application/connect+*` Content-Types. The HTTP handler buffers the request body for this
vertical slice (fine for demos; large uploads can move to chunked reads later).

## Errors

Throw `ConnectException(ConnectCode, message, details?)`. Packed `google.rpc` Any details are
carried as `ConnectErrorDetail(type, valueBase64)` and appear in Connect JSON `details[]`.
