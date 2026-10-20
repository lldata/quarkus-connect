# Chunked video/audio upload (no client streaming)

100MB+ uploads from browser/phone use **chunked unary** Connect RPCs on the same
HTTP port — not client streaming (Connect client streaming is awkward in browsers
and is out of scope for quarkus-connect v1 unary).

## Flow

1. Client picks a stable `client_request_id` (UUID) and persists it before the first byte.
2. `BeginUpload` — creates or **resumes** the session (idempotent).
3. `UploadChunk` — sends ~64–512 KiB unary chunks with `offset` + `chunk_index`.
   Re-sending the same `chunk_index` is a **no-op** (safe retries on EDGE / flaky Wi‑Fi).
4. After a drop or **app restart**, call `GetUploadStatus` and continue from `next_offset`.
5. `CompleteUpload` — finalizes a `Message` (duration + optional poster JPEG). Idempotent.

## Slow network (~EDGE)

Throttle the browser (Chrome DevTools → Network → Edge) or use `tc`/`toxiproxy`.
Because chunks are small unary calls:

- Progress = `bytes_received / total_size_bytes` after each chunk response.
- A failed chunk can be retried without corrupting the file.
- Timeouts should be per-chunk (`connect-timeout-ms`), not for the whole 100MB transfer.

## App restart mid-upload

Persist locally (e.g. `localStorage` / `shared_preferences`):

```json
{ "clientRequestId": "...", "totalSizeBytes": 104857600, "filename": "clip.mp4" }
```

On launch: if present → `GetUploadStatus` → resume `UploadChunk` from `next_offset`.
Never allocate a new `client_request_id` for the same logical upload.

## Demo (curl, small file)

```bash
ID=$(uuidgen)
# Begin
curl -sS http://localhost:8080/media.v1.MediaService/BeginUpload \
  -H 'Content-Type: application/json' -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d "{\"clientRequestId\":\"$ID\",\"filename\":\"a.bin\",\"contentType\":\"application/octet-stream\",\"totalSizeBytes\":\"6\",\"kind\":\"MEDIA_KIND_VIDEO\"}"

# One chunk (base64 of "hello!")
curl -sS http://localhost:8080/media.v1.MediaService/UploadChunk \
  -H 'Content-Type: application/json' -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d "{\"clientRequestId\":\"$ID\",\"offset\":\"0\",\"chunkIndex\":0,\"data\":\"aGVsbG8h\"}"

curl -sS http://localhost:8080/media.v1.MediaService/CompleteUpload \
  -H 'Content-Type: application/json' -H 'connect-protocol-version: 1' \
  -H 'Authorization: Bearer demo-user' \
  -d "{\"clientRequestId\":\"$ID\",\"durationMs\":\"1500\"}"
```

Then: `curl -i -H 'Range: bytes=0-2' http://localhost:8080/media/<messageId>`
