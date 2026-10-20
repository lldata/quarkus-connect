package io.github.lldata.connect.examples.media

import io.github.lldata.connect.examples.media.v1.BeginUploadRequest
import io.github.lldata.connect.examples.media.v1.BeginUploadResponse
import io.github.lldata.connect.examples.media.v1.CompleteUploadRequest
import io.github.lldata.connect.examples.media.v1.CompleteUploadResponse
import io.github.lldata.connect.examples.media.v1.GetMessageRequest
import io.github.lldata.connect.examples.media.v1.GetMessageResponse
import io.github.lldata.connect.examples.media.v1.GetUploadStatusRequest
import io.github.lldata.connect.examples.media.v1.GetUploadStatusResponse
import io.github.lldata.connect.examples.media.v1.ListMessagesRequest
import io.github.lldata.connect.examples.media.v1.ListMessagesResponse
import io.github.lldata.connect.examples.media.v1.MutinyMediaServiceGrpc
import io.github.lldata.connect.examples.media.v1.UploadChunkRequest
import io.github.lldata.connect.examples.media.v1.UploadChunkResponse
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject

/**
 * Chunked unary media upload — ordinary `@GrpcService`, no Connect-specific handlers.
 *
 * Designed for 100MB+ browser/phone uploads without client streaming: small unary chunks,
 * `client_request_id` idempotency, and [GetUploadStatus] for resume after drop / app restart.
 */
@GrpcService
class MediaGrpcService : MutinyMediaServiceGrpc.MediaServiceImplBase() {

    @Inject
    lateinit var store: UploadStore

    @Blocking
    override fun beginUpload(request: BeginUploadRequest): Uni<BeginUploadResponse> =
        Uni.createFrom().item {
            try {
                val result = store.begin(
                    clientRequestId = request.clientRequestId.trim(),
                    filename = request.filename,
                    contentType = request.contentType,
                    totalSizeBytes = request.totalSizeBytes,
                    kind = request.kind,
                )
                BeginUploadResponse.newBuilder()
                    .setUploadId(result.uploadId)
                    .setClientRequestId(result.clientRequestId)
                    .setNextOffset(result.nextOffset)
                    .setResumed(result.resumed)
                    .build()
            } catch (e: Exception) {
                throw mapError(e)
            }
        }

    @Blocking
    override fun uploadChunk(request: UploadChunkRequest): Uni<UploadChunkResponse> =
        Uni.createFrom().item {
            try {
                val result = store.writeChunk(
                    clientRequestId = request.clientRequestId.trim(),
                    offset = request.offset,
                    chunkIndex = request.chunkIndex,
                    data = request.data.toByteArray(),
                )
                UploadChunkResponse.newBuilder()
                    .setBytesReceived(result.bytesReceived)
                    .setNextOffset(result.nextOffset)
                    .build()
            } catch (e: Exception) {
                throw mapError(e)
            }
        }

    @Blocking
    override fun getUploadStatus(request: GetUploadStatusRequest): Uni<GetUploadStatusResponse> =
        Uni.createFrom().item {
            try {
                val status = store.status(request.clientRequestId.trim())
                GetUploadStatusResponse.newBuilder()
                    .setUploadId(status.uploadId)
                    .setBytesReceived(status.bytesReceived)
                    .setNextOffset(status.nextOffset)
                    .setTotalSizeBytes(status.totalSizeBytes)
                    .setCompleted(status.completed)
                    .setMessageId(status.messageId)
                    .build()
            } catch (e: Exception) {
                throw mapError(e)
            }
        }

    @Blocking
    override fun completeUpload(request: CompleteUploadRequest): Uni<CompleteUploadResponse> =
        Uni.createFrom().item {
            try {
                val message = store.complete(
                    clientRequestId = request.clientRequestId.trim(),
                    durationMs = request.durationMs,
                    posterJpeg = request.posterJpeg.toByteArray(),
                )
                CompleteUploadResponse.newBuilder().setMessage(message).build()
            } catch (e: Exception) {
                throw mapError(e)
            }
        }

    override fun getMessage(request: GetMessageRequest): Uni<GetMessageResponse> =
        Uni.createFrom().item {
            try {
                GetMessageResponse.newBuilder()
                    .setMessage(store.getMessage(request.messageId.trim()))
                    .build()
            } catch (e: Exception) {
                throw mapError(e)
            }
        }

    override fun listMessages(request: ListMessagesRequest): Uni<ListMessagesResponse> =
        Uni.createFrom().item {
            val limit = if (request.limit == 0) 50 else request.limit
            val messages = store.listMessages(request.afterSequence, limit)
            ListMessagesResponse.newBuilder()
                .addAllMessages(messages)
                .setLatestSequence(store.latestSequence())
                .build()
        }

    private fun mapError(e: Exception): StatusRuntimeException = when (e) {
        is UploadStore.NotFound -> Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        is UploadStore.InvalidArgument -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        is UploadStore.FailedPrecondition -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        is StatusRuntimeException -> e
        else -> Status.INTERNAL.withDescription(e.message ?: "internal").withCause(e).asRuntimeException()
    }
}
