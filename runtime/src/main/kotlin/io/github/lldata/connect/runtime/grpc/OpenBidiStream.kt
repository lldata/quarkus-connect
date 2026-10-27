package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.runtime.StreamCallControl
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Open bidi-streaming gRPC call: feed [trySend] as request envelopes arrive,
 * [halfClose] when the HTTP request ends; responses go to [observer].
 */
class OpenBidiStream<ReqT, RespT> internal constructor(
    descriptor: MethodDescriptor<ReqT, RespT>,
    handler: ServerCallHandler<ReqT, RespT>,
    context: ConnectCallContext,
    private val observer: ConnectStreamObserver<RespT>,
    callControl: StreamCallControl?,
    queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    private val onWritable: (() -> Unit)? = null,
) {
    private val finished = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val inbound = ArrayBlockingQueue<Any>(queueCapacity)
    private val queued = AtomicInteger(0)
    private val resumeAt = (queueCapacity / 2).coerceAtLeast(1)
    private val timeoutMs = context.timeoutMs ?: 60_000L
    private val deadlineCtx =
        Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, DEADLINE_SCHEDULER)
    private val previous: Context = deadlineCtx.attach()

    init {
        callControl?.attachGrpcCancel { deadlineCtx.cancel(Status.CANCELLED.asRuntimeException()) }
        val headers = toMetadata(context.requestHeaders)
        val call = object : ServerCall<ReqT, RespT>() {
            override fun request(numMessages: Int) = Unit

            override fun sendHeaders(headers: Metadata) {
                GrpcMetadataCopy.headersToContext(headers, context)
            }

            override fun sendMessage(message: RespT) {
                if (!finished.get() && callControl?.isCancelled() != true) {
                    observer.onNext(message)
                }
            }

            override fun close(status: Status, trailers: Metadata) {
                if (!finished.compareAndSet(false, true)) return
                try {
                    GrpcMetadataCopy.trailersToContext(trailers, context)
                    if (callControl?.isCancelled() == true) {
                        observer.onError(ConnectException(ConnectCode.CANCELED, "Client cancelled the stream"))
                    } else if (status.isOk) {
                        observer.onCompleted()
                    } else {
                        observer.onError(GrpcStatusMapping.toConnectException(status.asRuntimeException(trailers)))
                    }
                } finally {
                    deadlineCtx.detach(previous)
                    deadlineCtx.cancel(null)
                }
            }

            override fun isCancelled(): Boolean =
                deadlineCtx.isCancelled || finished.get() || callControl?.isCancelled() == true

            override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
        }

        WORKER.execute {
            try {
                val listener = handler.startCall(call, headers)
                listener.onReady()
                while (true) {
                    val item = inbound.take()
                    val left = queued.decrementAndGet()
                    if (left == resumeAt) {
                        onWritable?.invoke()
                    }
                    when (item) {
                        HalfClose -> {
                            listener.onHalfClose()
                            break
                        }
                        is CancelSignal -> {
                            listener.onCancel()
                            break
                        }
                        else -> {
                            @Suppress("UNCHECKED_CAST")
                            listener.onMessage(item as ReqT)
                        }
                    }
                }
            } catch (e: Exception) {
                if (finished.compareAndSet(false, true)) {
                    deadlineCtx.detach(previous)
                    deadlineCtx.cancel(null)
                    observer.onError(GrpcStatusMapping.toConnectException(e))
                }
            }
        }
    }

    fun trySend(message: ReqT): Boolean {
        if (closed.get()) return true
        queued.incrementAndGet()
        val ok = inbound.offer(message)
        if (!ok) {
            queued.decrementAndGet()
        }
        return ok
    }

    fun send(message: ReqT) {
        if (closed.get()) return
        queued.incrementAndGet()
        inbound.put(message)
    }

    fun queuedCount(): Int = queued.get()

    fun halfClose() {
        if (closed.compareAndSet(false, true)) {
            queued.incrementAndGet()
            inbound.put(HalfClose)
        }
    }

    fun cancel() {
        if (closed.compareAndSet(false, true)) {
            queued.incrementAndGet()
            if (!inbound.offer(CancelSignal)) {
                inbound.put(CancelSignal)
            }
        }
    }

    private object HalfClose
    private object CancelSignal

    companion object {
        const val DEFAULT_QUEUE_CAPACITY = 16

        private val DEADLINE_SCHEDULER = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "connect-bidi-stream-deadline").apply { isDaemon = true }
        }

        private val WORKER = java.util.concurrent.Executors.newCachedThreadPool { r ->
            Thread(r, "connect-bidi-stream-worker").apply { isDaemon = true }
        }

        private fun toMetadata(headers: Map<String, List<String>>): Metadata {
            val metadata = Metadata()
            for ((name, values) in headers) {
                val lower = name.lowercase()
                if (lower in HOP_BY_HOP || lower.startsWith(":")) continue
                if (lower.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
                val key = Metadata.Key.of(lower, Metadata.ASCII_STRING_MARSHALLER)
                for (value in values) {
                    metadata.put(key, value)
                }
            }
            return metadata
        }

        private val HOP_BY_HOP = setOf(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length",
            "content-type", "accept", "accept-encoding", "content-encoding",
            "connect-protocol-version", "connect-content-encoding",
            "connect-accept-encoding",
        )
    }
}
