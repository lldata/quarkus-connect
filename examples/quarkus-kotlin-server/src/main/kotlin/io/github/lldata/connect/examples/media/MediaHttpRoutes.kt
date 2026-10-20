package io.github.lldata.connect.examples.media

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import java.nio.file.Files

/**
 * HTTP range playback + poster on the same Quarkus HTTP port as Connect/gRPC.
 * Seek without full download via `Range: bytes=...`.
 */
@ApplicationScoped
class MediaHttpRoutes {

    @Inject
    lateinit var store: UploadStore

    fun register(@Observes router: Router) {
        router.get("/media/:id/poster").order(1_000).handler(this::poster)
        router.get("/media/:id").order(1_000).handler(this::media)
        router.head("/media/:id").order(1_000).handler(this::mediaHead)
    }

    private fun poster(ctx: RoutingContext) {
        try {
            val id = ctx.pathParam("id")
            val path = store.posterPath(id)
            val bytes = Files.readAllBytes(path)
            ctx.response()
                .putHeader("Content-Type", "image/jpeg")
                .putHeader("Cache-Control", "public, max-age=3600")
                .end(io.vertx.core.buffer.Buffer.buffer(bytes))
        } catch (e: UploadStore.NotFound) {
            ctx.response().setStatusCode(404).end(e.message)
        } catch (e: Exception) {
            ctx.response().setStatusCode(500).end(e.message)
        }
    }

    private fun mediaHead(ctx: RoutingContext) {
        try {
            val id = ctx.pathParam("id")
            val message = store.getMessage(id)
            val path = store.mediaPath(id)
            val size = Files.size(path)
            ctx.response()
                .putHeader("Content-Type", message.contentType)
                .putHeader("Accept-Ranges", "bytes")
                .putHeader("Content-Length", size.toString())
                .putHeader("Cache-Control", "public, max-age=3600")
                .end()
        } catch (e: UploadStore.NotFound) {
            ctx.response().setStatusCode(404).end()
        }
    }

    private fun media(ctx: RoutingContext) {
        try {
            val id = ctx.pathParam("id")
            val message = store.getMessage(id)
            val path = store.mediaPath(id)
            val size = Files.size(path)
            val rangeHeader = ctx.request().getHeader("Range")

            if (rangeHeader.isNullOrBlank()) {
                ctx.response()
                    .putHeader("Content-Type", message.contentType)
                    .putHeader("Accept-Ranges", "bytes")
                    .putHeader("Content-Length", size.toString())
                    .sendFile(path.toAbsolutePath().toString())
                return
            }

            val range = parseRange(rangeHeader, size)
                ?: run {
                    ctx.response()
                        .setStatusCode(416)
                        .putHeader("Content-Range", "bytes */$size")
                        .end()
                    return
                }

            val (start, end) = range
            val length = end - start + 1
            val channel = Files.newByteChannel(path)
            channel.position(start)
            val buf = ByteArray(length.toInt().coerceAtMost(1024 * 1024))
            // Stream in chunks for large ranges
            val response = ctx.response()
                .setStatusCode(206)
                .putHeader("Content-Type", message.contentType)
                .putHeader("Accept-Ranges", "bytes")
                .putHeader("Content-Range", "bytes $start-$end/$size")
                .putHeader("Content-Length", length.toString())
                .setChunked(false)

            var remaining = length
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = channel.read(java.nio.ByteBuffer.wrap(buf, 0, toRead))
                if (read < 0) break
                response.write(io.vertx.core.buffer.Buffer.buffer(buf.copyOf(read)))
                remaining -= read
            }
            channel.close()
            response.end()
        } catch (e: UploadStore.NotFound) {
            ctx.response().setStatusCode(404).end(e.message)
        } catch (e: Exception) {
            if (!ctx.response().headWritten()) {
                ctx.response().setStatusCode(500).end(e.message)
            }
        }
    }

    private fun parseRange(header: String, size: Long): Pair<Long, Long>? {
        // bytes=START-END | bytes=START- | bytes=-SUFFIX
        if (!header.startsWith("bytes=")) return null
        val spec = header.removePrefix("bytes=").trim()
        if ("," in spec) return null // multi-range not supported
        val parts = spec.split("-", limit = 2)
        if (parts.size != 2) return null
        return try {
            when {
                parts[0].isEmpty() && parts[1].isNotEmpty() -> {
                    val suffix = parts[1].toLong()
                    val start = (size - suffix).coerceAtLeast(0)
                    start to (size - 1)
                }
                parts[0].isNotEmpty() && parts[1].isEmpty() -> {
                    val start = parts[0].toLong()
                    if (start >= size) null else start to (size - 1)
                }
                else -> {
                    val start = parts[0].toLong()
                    val end = parts[1].toLong().coerceAtMost(size - 1)
                    if (start > end || start >= size) null else start to end
                }
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
