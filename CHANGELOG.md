# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/) once a non-SNAPSHOT
version is tagged.

## [Unreleased]

### Changed
- Development continues on `0.1.3-SNAPSHOT` after the `0.1.2` GitHub release

## [0.1.2] - 2026-10-20

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Fixed
- Enable **HTTP/2 cleartext (h2c)** in the conformance matrix for Connect (`supportsH2c`; gRPC-Web×H2 excluded pending quarkus-grpc port interaction)
- Conformance matrix **1018/1018** (Connect H1+H2 + gRPC-Web H1)

## [0.1.1] - 2026-10-20

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Fixed
- Preserve multi-value gRPC response headers/trailers through the Connect bridge (`Metadata.getAll` + Vert.x `headers().add`)
- Forward `connect-timeout-ms` into gRPC metadata so handlers can echo client deadlines (Deadline Propagation conformance)
- Flush Connect stream/error response headers (pending `sendHeaders` on close; leading metadata before first envelope)
- Align unexpected-request HTTP/status mapping with connect-go (405/415 empty bodies, stream cardinality, compressed+identity)
- gRPC-Web **streaming** (server/client/bidi) + compressed frames (`grpc-encoding` / gzip)
- Unpadded base64 for binary trailers (`grpc-status-details-bin`); echo `grpc-timeout` for Deadline Propagation

### Conformance
- Connect-only thin matrix **364/364**
- Connect + gRPC-Web HTTP/1 matrix **654/654** (no known-failing entries)

## [0.1.0] - 2026-10-20

First public GitHub release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Added
- Connect unary + server streaming + gRPC-Web for ordinary `@GrpcService` beans on one HTTP port
- Chunked media upload example, Flutter web client, OTel interceptor example
- Maven Central publish wiring (`com.vanniktech.maven.publish`); tag workflow fail-closed without secrets
- Agent tend/issue/review/claude house-style workflows (skip until Claude token/app exist)
- Unary GET `connect=v1` / `base64=1` / `Vary: Accept-Encoding`; Java `ConnectUnaryMethod` SPI example
- Clear `unimplemented` for client/bidi streaming RPCs hit over Connect
- `docs/COMPLIANCE.md` matrix; GET unknown `encoding` → HTTP 415; advertise `Accept-Encoding: identity`
- Reject non-identity `connect-content-encoding` on Connect streams (parity with unary `Content-Encoding`)
- **gzip** request/response compression for Connect unary and server streaming (`Content-Encoding` / `Accept-Encoding`, GET `compression=`, `connect-content-encoding` / `connect-accept-encoding`)
- Connect **client streaming** for `@GrpcService` `CLIENT_STREAMING` methods
- Connect **bidi streaming** for `@GrpcService` `BIDI_STREAMING` methods (buffered vertical slice)
- Document why Connect message-coding **brotli (`br`)** is deferred (native libs; gzip remains)
- Curl interop matrix (`scripts/interop-matrix.sh` + Gradle `interopMatrix` in `check`) and streaming cancel/`connect-timeout-ms` hardening
- Thin `conformance/` ConformanceService host + `connectconformance` suite in `check` (partial matrix; known-failing documented)
- JSON codec registers protobuf `Any` types from the message file descriptor (needed for conformance payloads)
- GitHub-only release packaging script (`scripts/package-github-release.sh`)

[Unreleased]: https://github.com/lldata/quarkus-connect/compare/v0.1.2...HEAD
[0.1.2]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.2
[0.1.1]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.1
[0.1.0]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.0
