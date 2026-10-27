# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/) once a non-SNAPSHOT
version is tagged.

## [Unreleased]

### Changed
- Development continues on `0.1.8-SNAPSHOT` after the `0.1.7` release

### Fixed
- Publish promote rewrites `META-INF/quarkus-extension.properties` / `.yaml` (and refuses
  leftover `-SNAPSHOT`) so a released runtime jar points at the release deployment
  artifact, not `*-SNAPSHOT` (#63). Do not re-upload broken `0.1.7`; ship `0.1.8`.
- Connect unary GET is served only for methods with `idempotency_level = NO_SIDE_EFFECTS`
  (otherwise HTTP 405 `Allow: POST`, matching connect-go) (#51)

## [0.1.7] - 2026-10-27

Promoted from `0.1.7-SNAPSHOT` (CI snapshot artifact; Publish workflow, no rebuild).

### Added
- Flutter example: TokenProvider, IdleAwareTimeout, and reading error trailers
  (`retry-after-seconds` on guest WhoAmI) (#45)
- `quarkus.connect.max-inbound-message-size` (default 4 MiB): bound Connect unary bodies and
  each Connect / gRPC-Web streaming envelope; oversize → `resource_exhausted` (#44). Conformance
  `supportsMessageReceiveLimit` enabled. Native gRPC also enforces the limit post-decompress
  via `ConnectGrpcMaxInboundInterceptor` (gzip wire size can be under Netty's framed limit).
- Client and bidi Connect / gRPC-Web request bodies stream envelopes into the gRPC bridge as
  they arrive (Vert.x pause/resume back-pressure); no full-body buffer (#43).

### Changed
- Development continues on `0.1.7-SNAPSHOT` after the `0.1.6` GitHub release
- Publish is manual/`workflow_dispatch` only: CI stages SNAPSHOT Maven artifacts on `main`;
  the Publish workflow promotes the newest snapshot to a release (no rebuild) using
  Environment `publish` secrets

## [0.1.6] - 2026-10-26

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Added
- `quarkus.connect.json.preserve-proto-field-names` (runtime, default `false`): print Connect /
  gRPC-Web JSON responses with the proto field names instead of lowerCamelCase (#46 / #47)
- Docs: JVM Connect client is official Connect-Kotlin — this extension stays server-only (#41)

## [0.1.5] - 2026-10-20

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Added
- **Opt-in Quarkus Security** for Connect / gRPC-Web: propagate `SecurityIdentity`, honor
  `@RolesAllowed` / `@Authenticated` when the app adds `quarkus-security` (+ mechanism);
  no hard Security/OIDC/JWT deps on Connect core. Covered by `:integration-tests-security`.

## [0.1.4] - 2026-10-20

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Added
- Enable **native gRPC** (`PROTOCOL_GRPC`, proto) in the conformance matrix on h2c
  (exclude gRPC×JSON; known-fail unexpected-request mapping; known-flaky Vert.x padded
  `grpc-status-details-bin` on Errors/Duplicate Metadata)
- Conformance matrix **~1514 pass** (+ native gRPC proto on h2c)

## [0.1.3] - 2026-10-20

GitHub-only release (no Maven Central — secrets not configured; see `docs/RELEASE.md` Path A).

### Fixed
- Claim gRPC-Web before quarkus-grpc on the shared HTTP port so `application/grpc-web*` is not
  stolen by `isGrpc` (`^application/grpc.*`) on HTTP/2 (GOAWAY / `application/grpc` content-type)
- Enable **gRPC-Web over HTTP/2 h2c** in the conformance matrix (drop `exclude_cases`)
- Conformance matrix **1308/1308** (Connect + gRPC-Web on H1 and H2)

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

[Unreleased]: https://github.com/lldata/quarkus-connect/compare/v0.1.4...HEAD
[0.1.4]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.4
[0.1.3]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.3
[0.1.2]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.2
[0.1.1]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.1
[0.1.0]: https://github.com/lldata/quarkus-connect/releases/tag/v0.1.0
