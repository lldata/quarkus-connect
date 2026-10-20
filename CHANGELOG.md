# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/) once a non-SNAPSHOT
version is tagged.

## [Unreleased]

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

### Notes
- Version remains `999-SNAPSHOT` until a human tags a release (see `docs/RELEASE.md`)
