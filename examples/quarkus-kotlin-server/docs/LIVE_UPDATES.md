# Live updates — Connect streaming + polling

## Decision

**Prefer Connect server streaming** (`WatchMessages`) when the client stays online;
**fall back to unary `ListMessages` polling** after drop / app restart / reconnect.

Both use the same `@GrpcService` on one HTTP port.

## Connect server streaming

`WatchMessages(after_sequence)` is a **server-streaming** RPC exposed as
`application/connect+json` or `application/connect+proto`.

Envelope framing (Connect protocol):

1. Request body: one data envelope (flag `0x00` + 4-byte BE length + payload).
2. Response: zero or more data envelopes, then an end-stream envelope (flag `0x02`)
   with optional JSON `{ "error": …, "metadata": … }`.

Catch-up semantics: the server streams messages with `sequence > after_sequence`
(up to `limit`), then completes. The client reconnects with the last seen sequence
for continuous live updates — same resume shape as upload / polling.

```bash
# After at least one CompleteUpload — stream catch-up as Connect JSON envelopes:
printf '\x00\x00\x00\x00\x15{"afterSequence":"0"}' | \
  curl -sS -X POST http://localhost:8080/media.v1.MediaService/WatchMessages \
    -H 'Content-Type: application/connect+json' \
    -H 'connect-protocol-version: 1' \
    -H 'Authorization: Bearer demo-user' \
    --data-binary @-
```

## Unary polling (always available)

```text
seq = 0
loop every 1–2s (backoff when idle):
  res = ListMessages(after_sequence=seq, limit=50)
  append res.messages
  seq = max(seq, res.latest_sequence)
```

Use polling when:

- the transport cannot keep a long-lived stream (some mobile backgrounds),
- you only need infrequent catch-up,
- or you prefer each poll to be a normal unary RPC (Bearer + OTel span per call).

## Client reconnect pattern (streaming)

```text
seq = 0
loop:
  stream = WatchMessages(after_sequence=seq)
  for each message in stream:
    append message
    seq = message.sequence
  // stream completed or dropped → reconnect with same seq
```

Interceptors and OTel apply to both streaming and unary paths.
