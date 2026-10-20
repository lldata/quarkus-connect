# examples/flutter-web-client

Flutter app (web + iOS/Android) using the [Connect](https://connectrpc.com/) protocol
via [`connectrpc`](https://pub.dev/packages/connectrpc) against
[`examples/quarkus-kotlin-server`](../quarkus-kotlin-server).

Dart stubs are generated from the **same** `demo/v1/demo.proto` as the server
(`buf generate`). Generated sources under `lib/src/gen/` are committed so the
example runs without a Buf install.

## Prerequisites

- Flutter 3.24+ (Dart 3.5+)
- Running server: `./gradlew :examples:quarkus-kotlin-server:quarkusDev`

## Run (web)

```bash
cd examples/flutter-web-client
flutter pub get
flutter run -d chrome
```

Default base URL: `http://localhost:8080`. Tokens: `demo-user`, `demo-guest`.

## Regenerate Dart from proto

```bash
# from this directory; needs Buf CLI
buf generate
```

`buf.gen.yaml` uses remote plugins: `protocolbuffers/dart` + `connectrpc/dart`.

## Media upload + live updates

The home screen can **pick a file** and upload via chunked unary Connect RPCs
(`MediaService`), persisting `client_request_id` in `shared_preferences` so a
reload mid-upload can resume.

Live updates:

- **Watch stream** — Connect server streaming `WatchMessages` (`application/connect+json`)
- **Poll messages** — unary `ListMessages` fallback

See server `docs/LIVE_UPDATES.md` for reconnect semantics.

```bash
buf generate   # after editing proto/media/v1/media.proto
```

## Mobile note

Web uses `package:connectrpc/web.dart` (`fetch`). For iOS/Android, switch the
HTTP client import to `package:connectrpc/io.dart` or `http2.dart` — same
generated clients and Connect JSON/binary transport (path to drop grpc-dart).
