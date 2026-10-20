package io.github.lldata.connect.conformance

import com.google.protobuf.ByteString
import connectrpc.conformance.v1.BidiStreamRequest
import connectrpc.conformance.v1.BidiStreamResponse
import connectrpc.conformance.v1.ClientStreamRequest
import connectrpc.conformance.v1.ClientStreamResponse
import connectrpc.conformance.v1.ConformancePayload
import connectrpc.conformance.v1.IdempotentUnaryRequest
import connectrpc.conformance.v1.IdempotentUnaryResponse
import connectrpc.conformance.v1.MutinyConformanceServiceGrpc
import connectrpc.conformance.v1.ServerStreamRequest
import connectrpc.conformance.v1.ServerStreamResponse
import connectrpc.conformance.v1.StreamResponseDefinition
import connectrpc.conformance.v1.UnaryRequest
import connectrpc.conformance.v1.UnaryResponse
import connectrpc.conformance.v1.UnaryResponseDefinition
import io.github.lldata.connect.conformance.ConformanceSupport.applyStreamMeta
import io.github.lldata.connect.conformance.ConformanceSupport.applyUnaryMeta
import io.github.lldata.connect.conformance.ConformanceSupport.buildRequestInfo
import io.github.lldata.connect.conformance.ConformanceSupport.payload
import io.github.lldata.connect.conformance.ConformanceSupport.sleepMs
import io.github.lldata.connect.conformance.ConformanceSupport.statusRuntimeFrom
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thin [connectrpc.conformance.v1.ConformanceService] for the official runner.
 *
 * [Unimplemented] is intentionally not overridden (framework returns `unimplemented`).
 * Full-duplex interactive half-close is limited by buffered Connect client/bidi bodies —
 * see config/known-failing.txt and docs/INTEROP.md.
 */
@GrpcService
class ConformanceGrpcService : MutinyConformanceServiceGrpc.ConformanceServiceImplBase() {

    @Blocking
    override fun unary(request: UnaryRequest): Uni<UnaryResponse> =
        Uni.createFrom().item { handleUnary(request.responseDefinition, listOf(request)) }

    @Blocking
    override fun idempotentUnary(request: IdempotentUnaryRequest): Uni<IdempotentUnaryResponse> =
        Uni.createFrom().item {
            // Connect GET query params are not always visible to @GrpcService code; empty is allowed.
            val getInfo = ConformancePayload.ConnectGetInfo.getDefaultInstance()
            val definition = request.responseDefinition
            applyUnaryMeta(definition)
            val requestInfo = buildRequestInfo(listOf(request), getInfo)

            if (definition != null && definition.hasError()) {
                sleepMs(definition.responseDelayMs)
                throw statusRuntimeFrom(definition.error, listOf(requestInfo))
            }

            val data =
                if (definition != null && definition.hasResponseData()) {
                    definition.responseData
                } else {
                    ByteString.EMPTY
                }
            sleepMs(definition?.responseDelayMs ?: 0)
            IdempotentUnaryResponse.newBuilder()
                .setPayload(payload(data, requestInfo))
                .build()
        }

    @Blocking
    override fun clientStream(requests: Multi<ClientStreamRequest>): Uni<ClientStreamResponse> =
        requests.collect().asList().onItem().transform { list ->
            val definition =
                list.firstOrNull()?.responseDefinition ?: UnaryResponseDefinition.getDefaultInstance()
            applyUnaryMeta(definition)
            val requestInfo = buildRequestInfo(list)

            if (definition.hasError()) {
                sleepMs(definition.responseDelayMs)
                throw statusRuntimeFrom(definition.error, listOf(requestInfo))
            }

            val data =
                if (definition.hasResponseData()) definition.responseData else ByteString.EMPTY
            sleepMs(definition.responseDelayMs)
            ClientStreamResponse.newBuilder()
                .setPayload(payload(data, requestInfo))
                .build()
        }

    @Blocking
    override fun serverStream(request: ServerStreamRequest): Multi<ServerStreamResponse> {
        val definition = request.responseDefinition ?: StreamResponseDefinition.getDefaultInstance()
        applyStreamMeta(definition)
        val requestInfo = buildRequestInfo(listOf(request))

        if (definition.responseDataCount == 0) {
            return if (definition.hasError()) {
                Multi.createFrom().failure(statusRuntimeFrom(definition.error, listOf(requestInfo)))
            } else {
                Multi.createFrom().empty()
            }
        }

        val first = AtomicBoolean(true)
        var multi: Multi<ServerStreamResponse> =
            Multi.createFrom().iterable(definition.responseDataList)
                .onItem().transform { data ->
                    sleepMs(definition.responseDelayMs)
                    val info = if (first.compareAndSet(true, false)) requestInfo else null
                    ServerStreamResponse.newBuilder()
                        .setPayload(payload(data, info))
                        .build()
                }
        if (definition.hasError()) {
            multi = multi.onCompletion().failWith { statusRuntimeFrom(definition.error) }
        }
        return multi
    }

    @Blocking
    override fun bidiStream(requests: Multi<BidiStreamRequest>): Multi<BidiStreamResponse> {
        // Buffered Connect path: collect then respond (half-duplex semantics even if full_duplex=true).
        // Full-duplex interactive cases are listed in known-failing.txt.
        return requests.collect().asList().onItem().transformToMulti { list ->
            if (list.isEmpty()) {
                return@transformToMulti Multi.createFrom().empty()
            }
            val first = list.first()
            val definition = first.responseDefinition ?: StreamResponseDefinition.getDefaultInstance()
            val fullDuplex = first.fullDuplex
            applyStreamMeta(definition)
            val allInfo = buildRequestInfo(list)

            if (definition.responseDataCount == 0) {
                return@transformToMulti if (definition.hasError()) {
                    Multi.createFrom().failure(statusRuntimeFrom(definition.error, listOf(allInfo)))
                } else {
                    Multi.createFrom().empty()
                }
            }

            if (fullDuplex) {
                // Best-effort: pair each request with a response when counts allow.
                val responses = mutableListOf<BidiStreamResponse>()
                val sent = AtomicInteger(0)
                for ((index, req) in list.withIndex()) {
                    if (index >= definition.responseDataCount) break
                    sleepMs(definition.responseDelayMs)
                    val info =
                        if (sent.get() == 0) {
                            buildRequestInfo(listOf(req)).toBuilder()
                                .clearRequests()
                                .addAllRequests(buildRequestInfo(list.take(index + 1)).requestsList)
                                .build()
                        } else {
                            buildRequestInfo(listOf(req))
                        }
                    responses +=
                        BidiStreamResponse.newBuilder()
                            .setPayload(payload(definition.getResponseData(index), info))
                            .build()
                    sent.incrementAndGet()
                }
                var multi: Multi<BidiStreamResponse> = Multi.createFrom().iterable(responses)
                if (definition.hasError()) {
                    multi = multi.onCompletion().failWith { statusRuntimeFrom(definition.error) }
                }
                multi
            } else {
                val firstSent = AtomicBoolean(true)
                var multi: Multi<BidiStreamResponse> =
                    Multi.createFrom().iterable(definition.responseDataList)
                        .onItem().transform { data ->
                            sleepMs(definition.responseDelayMs)
                            val info = if (firstSent.compareAndSet(true, false)) allInfo else null
                            BidiStreamResponse.newBuilder()
                                .setPayload(payload(data, info))
                                .build()
                        }
                if (definition.hasError()) {
                    multi = multi.onCompletion().failWith { statusRuntimeFrom(definition.error) }
                }
                multi
            }
        }
    }

    private fun handleUnary(
        definition: UnaryResponseDefinition?,
        requests: List<com.google.protobuf.Message>,
    ): UnaryResponse {
        applyUnaryMeta(definition)
        val requestInfo = buildRequestInfo(requests)

        if (definition != null && definition.hasError()) {
            sleepMs(definition.responseDelayMs)
            throw statusRuntimeFrom(definition.error, listOf(requestInfo))
        }

        val data =
            if (definition != null && definition.hasResponseData()) {
                definition.responseData
            } else {
                ByteString.EMPTY
            }
        sleepMs(definition?.responseDelayMs ?: 0)
        return UnaryResponse.newBuilder()
            .setPayload(payload(data, requestInfo))
            .build()
    }
}
