# Production ingress (single port)

Serve **one HTTPS port** for browsers (HTTP/1.1 Connect JSON / gRPC-Web) and phones
(HTTP/2 Connect or native gRPC). Quarkus setting:

```properties
quarkus.grpc.server.use-separate-server=false
quarkus.http.enable-compression=true
quarkus.http.compress-media-types=application/json,application/proto,text/plain
```

## CORS

Demo allows any origin (`/.*/`). For production, pin exact SPA origins:

```properties
quarkus.http.cors=true
quarkus.http.cors.origins=https://app.example.com
quarkus.http.cors.headers=accept,authorization,content-type,connect-protocol-version,connect-timeout-ms,range
quarkus.http.cors.methods=GET,POST,OPTIONS,HEAD
quarkus.http.cors.exposed-headers=content-range,accept-ranges,content-length
```

Flutter web on another origin must send `Authorization` and read `Content-Range`
for media seek — hence `exposed-headers`.

## Azure Container Apps

- Ingress: external, target port **8080** (Quarkus HTTP; gRPC not on a second port).
- HTTP/2: enable on the ingress when native gRPC or h2 Connect from mobile is required.
- TLS terminates at ACA; app can stay plain HTTP inside the revision.
- Set CORS origins to the Static Web Apps / Front Door hostname.
- Increase request body limits if large unary chunks are used (prefer ≤1 MiB chunks).

## Cloudflare

- Proxied HTTPS to origin 8080.
- For native gRPC through Cloudflare, use gRPC support / HTTP/2 to origin; Connect
  JSON and gRPC-Web work over HTTP/1.1 without special gRPC mode.
- Enable Brotli/gzip at the edge; Quarkus compression covers origin JSON.
- Cache **only** `GET /media/*/poster` if desired; never cache `UploadChunk` / Connect POST.

## OpenTelemetry

`RpcTracingInterceptor` creates one span per RPC with procedure name + status
(no payloads). Example defaults `quarkus.otel.traces.exporter=none` for local
dev; tests use the **logging** exporter so CI proves spans without a collector.

Point production at OTLP for ACA/Cloudflare-adjacent collectors:

```properties
quarkus.otel.exporter.otlp.endpoint=http://otel-collector:4317
quarkus.otel.traces.exporter=otlp
```
