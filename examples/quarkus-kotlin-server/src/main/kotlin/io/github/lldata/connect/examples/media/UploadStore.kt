package io.github.lldata.connect.examples.media

import io.github.lldata.connect.examples.media.v1.MediaKind
import io.github.lldata.connect.examples.media.v1.Message
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Disk-backed chunked upload store with `client_request_id` idempotency.
 *
 * Resume: [status] returns `next_offset`; re-uploading an already-written chunk is a no-op.
 * Complete is idempotent — a second Complete for the same id returns the same [Message].
 */
@ApplicationScoped
class UploadStore {

    @ConfigProperty(name = "demo.media.dir", defaultValue = "/tmp/quarkus-connect-media")
    lateinit var mediaDirProp: String

    private val sequence = AtomicLong(0)
    private val uploads = ConcurrentHashMap<String, UploadSession>()
    private val messagesById = ConcurrentHashMap<String, Message>()
    private val messagesByClientId = ConcurrentHashMap<String, Message>()

    private fun root(): Path {
        val root = Path.of(mediaDirProp)
        Files.createDirectories(root)
        return root
    }

    fun begin(
        clientRequestId: String,
        filename: String,
        contentType: String,
        totalSizeBytes: Long,
        kind: MediaKind,
    ): BeginResult {
        require(clientRequestId.isNotBlank()) { "client_request_id required" }
        require(totalSizeBytes > 0) { "total_size_bytes must be > 0" }
        require(totalSizeBytes <= MAX_BYTES) { "total_size_bytes exceeds $MAX_BYTES" }

        messagesByClientId[clientRequestId]?.let { existing ->
            return BeginResult(
                uploadId = existing.messageId,
                clientRequestId = clientRequestId,
                nextOffset = existing.sizeBytes,
                resumed = true,
                alreadyCompleted = true,
            )
        }

        val existing = uploads[clientRequestId]
        if (existing != null) {
            return BeginResult(
                uploadId = existing.uploadId,
                clientRequestId = clientRequestId,
                nextOffset = existing.bytesReceived,
                resumed = true,
                alreadyCompleted = false,
            )
        }

        val uploadId = UUID.randomUUID().toString()
        val session = UploadSession(
            uploadId = uploadId,
            clientRequestId = clientRequestId,
            filename = filename.ifBlank { "upload.bin" },
            contentType = contentType.ifBlank { "application/octet-stream" },
            totalSizeBytes = totalSizeBytes,
            kind = if (kind == MediaKind.MEDIA_KIND_UNSPECIFIED) MediaKind.MEDIA_KIND_VIDEO else kind,
            path = root().resolve("$uploadId.bin"),
        )
        Files.write(session.path, ByteArray(0), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        uploads[clientRequestId] = session
        return BeginResult(uploadId, clientRequestId, 0, resumed = false, alreadyCompleted = false)
    }

    fun status(clientRequestId: String): StatusResult {
        messagesByClientId[clientRequestId]?.let { msg ->
            return StatusResult(
                uploadId = msg.messageId,
                bytesReceived = msg.sizeBytes,
                nextOffset = msg.sizeBytes,
                totalSizeBytes = msg.sizeBytes,
                completed = true,
                messageId = msg.messageId,
            )
        }
        val session = uploads[clientRequestId]
            ?: throw NotFound("unknown client_request_id")
        return StatusResult(
            uploadId = session.uploadId,
            bytesReceived = session.bytesReceived,
            nextOffset = session.bytesReceived,
            totalSizeBytes = session.totalSizeBytes,
            completed = false,
            messageId = "",
        )
    }

    @Synchronized
    fun writeChunk(clientRequestId: String, offset: Long, chunkIndex: Int, data: ByteArray): ChunkResult {
        val session = uploads[clientRequestId]
            ?: throw NotFound("unknown client_request_id")
        if (session.completedMessageId != null) {
            return ChunkResult(session.bytesReceived, session.bytesReceived)
        }
        if (data.isEmpty()) {
            throw InvalidArgument("chunk data must not be empty")
        }
        if (offset + data.size > session.totalSizeBytes) {
            throw InvalidArgument("chunk exceeds total_size_bytes")
        }

        // Idempotent retry of an already-applied chunk.
        if (chunkIndex in session.appliedChunks) {
            return ChunkResult(session.bytesReceived, session.bytesReceived)
        }

        if (offset != session.bytesReceived) {
            throw FailedPrecondition(
                "expected offset ${session.bytesReceived}, got $offset (call GetUploadStatus to resume)",
            )
        }

        Files.write(session.path, data, StandardOpenOption.APPEND)
        session.bytesReceived += data.size
        session.appliedChunks.add(chunkIndex)
        return ChunkResult(session.bytesReceived, session.bytesReceived)
    }

    @Synchronized
    fun complete(clientRequestId: String, durationMs: Long, posterJpeg: ByteArray): Message {
        messagesByClientId[clientRequestId]?.let { return it }

        val session = uploads[clientRequestId]
            ?: throw NotFound("unknown client_request_id")
        if (session.bytesReceived != session.totalSizeBytes) {
            throw FailedPrecondition(
                "upload incomplete: received ${session.bytesReceived} of ${session.totalSizeBytes}",
            )
        }

        val messageId = session.uploadId
        if (posterJpeg.isNotEmpty()) {
            Files.write(root().resolve("$messageId.poster.jpg"), posterJpeg)
        } else {
            Files.write(root().resolve("$messageId.poster.jpg"), PLACEHOLDER_JPEG)
        }

        val seq = sequence.incrementAndGet()
        val message = Message.newBuilder()
            .setMessageId(messageId)
            .setSequence(seq)
            .setKind(session.kind)
            .setFilename(session.filename)
            .setContentType(session.contentType)
            .setSizeBytes(session.totalSizeBytes)
            .setDurationMs(durationMs.coerceAtLeast(0))
            .setMediaUrl("/media/$messageId")
            .setPosterUrl("/media/$messageId/poster")
            .setClientRequestId(clientRequestId)
            .setCreatedUnixMs(System.currentTimeMillis())
            .build()

        messagesById[messageId] = message
        messagesByClientId[clientRequestId] = message
        session.completedMessageId = messageId
        return message
    }

    fun getMessage(messageId: String): Message =
        messagesById[messageId] ?: throw NotFound("message not found")

    fun listMessages(afterSequence: Long, limit: Int): List<Message> {
        val capped = limit.coerceIn(1, 100)
        return messagesById.values
            .asSequence()
            .filter { it.sequence > afterSequence }
            .sortedBy { it.sequence }
            .take(capped)
            .toList()
    }

    fun latestSequence(): Long = sequence.get()

    fun mediaPath(messageId: String): Path {
        getMessage(messageId)
        val path = root().resolve("$messageId.bin")
        if (!Files.isRegularFile(path)) throw NotFound("media blob missing")
        return path
    }

    fun posterPath(messageId: String): Path {
        getMessage(messageId)
        val path = root().resolve("$messageId.poster.jpg")
        if (!Files.isRegularFile(path)) throw NotFound("poster missing")
        return path
    }

    data class BeginResult(
        val uploadId: String,
        val clientRequestId: String,
        val nextOffset: Long,
        val resumed: Boolean,
        val alreadyCompleted: Boolean,
    )

    data class StatusResult(
        val uploadId: String,
        val bytesReceived: Long,
        val nextOffset: Long,
        val totalSizeBytes: Long,
        val completed: Boolean,
        val messageId: String,
    )

    data class ChunkResult(val bytesReceived: Long, val nextOffset: Long)

    private class UploadSession(
        val uploadId: String,
        val clientRequestId: String,
        val filename: String,
        val contentType: String,
        val totalSizeBytes: Long,
        val kind: MediaKind,
        val path: Path,
        var bytesReceived: Long = 0,
        val appliedChunks: MutableSet<Int> = concurrentSet(),
        var completedMessageId: String? = null,
    )

    class NotFound(message: String) : RuntimeException(message)
    class InvalidArgument(message: String) : RuntimeException(message)
    class FailedPrecondition(message: String) : RuntimeException(message)

    companion object {
        const val MAX_BYTES: Long = 512L * 1024L * 1024L // 512 MiB cap for the demo
        private fun concurrentSet(): MutableSet<Int> =
            java.util.Collections.newSetFromMap(ConcurrentHashMap())

        // Minimal valid JPEG (1x1 pixel) used when the client omits a poster.
        private val PLACEHOLDER_JPEG: ByteArray = listOf(
            0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01,
            0x00, 0x01, 0x00, 0x00, 0xFF, 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08,
            0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12,
            0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20,
            0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34, 0x1F, 0x27,
            0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32, 0xFF, 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01,
            0x00, 0x01, 0x01, 0x01, 0x11, 0x00, 0xFF, 0xC4, 0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xFF, 0xC4, 0x00, 0x14,
            0x10, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0xFF, 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00, 0x37, 0xFF, 0xD9,
        ).map { it.toByte() }.toByteArray()
    }
}
