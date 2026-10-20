# Live updates — polling preferred (v1)

## Decision

**Prefer unary `ListMessages` polling** for new messages in a thread, rather than
Connect server streaming, for this stack in v1.

## Why

1. **Extension scope** — quarkus-connect v1 implements **unary** Connect + gRPC-Web.
   Connect server streaming needs `application/connect+json|proto` envelope framing,
   half-close semantics, and backpressure. That is explicitly deferred in architecture.
2. **Flutter web** — `connectrpc` web transport supports server streaming, but only once
   the server speaks Connect streaming. Until then, polling works on HTTP/1.1 and HTTP/2
   through the same ingress (ACA / Cloudflare) without sticky upgrade requirements.
3. **Resume / mobile** — polling with `after_sequence` mirrors the upload resume story:
   app restart → last seen sequence → `ListMessages`. No half-open stream to recover.
4. **Interceptors / auth** — each poll is a normal unary RPC; Bearer interceptors and
   OTel spans apply unchanged.

## Client pattern

```text
seq = 0
loop every 1–2s (backoff when idle):
  res = ListMessages(after_sequence=seq, limit=50)
  append res.messages
  seq = max(seq, res.latest_sequence)
```

Optional: long-poll later by adding a server-side wait with `connect-timeout-ms`,
still unary.

## When to revisit streaming

After quarkus-connect grows Connect server-streaming support, `WatchMessages` can
replace the poll loop for browsers that benefit from push — same `@GrpcService`
surface, same port.
