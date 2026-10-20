package io.github.lldata.connect.conformance

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.rpc.Status as RpcStatus
import connectrpc.conformance.v1.Code
import connectrpc.conformance.v1.ConformancePayload
import connectrpc.conformance.v1.Error as ConformanceError
import connectrpc.conformance.v1.Header
import connectrpc.conformance.v1.StreamResponseDefinition
import connectrpc.conformance.v1.UnaryResponseDefinition
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.protobuf.StatusProto

internal object ConformanceSupport {
    fun requestHeadersFromContext(): List<Header> {
        val metadata = ConformanceGrpcKeys.REQUEST_HEADERS.get(Context.current()) ?: return emptyList()
        val grouped = linkedMapOf<String, MutableList<String>>()
        for (key in metadata.keys()) {
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
            val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
            val values = metadata.getAll(metaKey) ?: continue
            grouped.getOrPut(key) { mutableListOf() }.addAll(values)
        }
        return grouped.map { (name, values) ->
            Header.newBuilder().setName(name).addAllValue(values).build()
        }
    }

    fun buildRequestInfo(
        requests: List<Message>,
        connectGetInfo: ConformancePayload.ConnectGetInfo? = null,
    ): ConformancePayload.RequestInfo {
        val builder =
            ConformancePayload.RequestInfo.newBuilder()
                .addAllRequestHeaders(requestHeadersFromContext())
                .addAllRequests(requests.map { Any.pack(it) })
        // Do not echo timeout_ms: the Connect bridge always attaches a gRPC Context deadline
        // (default 30s), which is not distinguishable from a client-provided connect-timeout-ms.
        if (connectGetInfo != null) {
            builder.connectGetInfo = connectGetInfo
        }
        return builder.build()
    }

    fun applyUnaryMeta(definition: UnaryResponseDefinition?) {
        if (definition == null) return
        val pending = ConformanceGrpcKeys.PENDING.get(Context.current()) ?: return
        if (definition.responseHeadersCount > 0) {
            pending.headers = toMetadata(definition.responseHeadersList)
        }
        if (definition.responseTrailersCount > 0) {
            pending.trailers = toMetadata(definition.responseTrailersList)
        }
    }

    fun applyStreamMeta(definition: StreamResponseDefinition?) {
        if (definition == null) return
        val pending = ConformanceGrpcKeys.PENDING.get(Context.current()) ?: return
        if (definition.responseHeadersCount > 0) {
            pending.headers = toMetadata(definition.responseHeadersList)
        }
        if (definition.responseTrailersCount > 0) {
            pending.trailers = toMetadata(definition.responseTrailersList)
        }
    }

    fun toMetadata(headers: List<Header>): Metadata {
        val metadata = Metadata()
        for (header in headers) {
            val key = Metadata.Key.of(header.name.lowercase(), Metadata.ASCII_STRING_MARSHALLER)
            for (value in header.valueList) {
                metadata.put(key, value)
            }
        }
        return metadata
    }

    fun sleepMs(ms: Int) {
        if (ms <= 0) return
        Thread.sleep(ms.toLong())
    }

    fun statusRuntimeFrom(
        error: ConformanceError,
        extraDetails: List<Message> = emptyList(),
    ): RuntimeException {
        val rpc =
            RpcStatus.newBuilder()
                .setCode(toGrpcStatus(error.code).code.value())
                .setMessage(if (error.hasMessage()) error.message else "")
        for (detail in error.detailsList) {
            rpc.addDetails(detail)
        }
        for (extra in extraDetails) {
            rpc.addDetails(Any.pack(extra))
        }
        return StatusProto.toStatusRuntimeException(rpc.build())
    }

    fun toGrpcStatus(code: Code): Status =
        when (code) {
            Code.CODE_CANCELED -> Status.CANCELLED
            Code.CODE_UNKNOWN -> Status.UNKNOWN
            Code.CODE_INVALID_ARGUMENT -> Status.INVALID_ARGUMENT
            Code.CODE_DEADLINE_EXCEEDED -> Status.DEADLINE_EXCEEDED
            Code.CODE_NOT_FOUND -> Status.NOT_FOUND
            Code.CODE_ALREADY_EXISTS -> Status.ALREADY_EXISTS
            Code.CODE_PERMISSION_DENIED -> Status.PERMISSION_DENIED
            Code.CODE_RESOURCE_EXHAUSTED -> Status.RESOURCE_EXHAUSTED
            Code.CODE_FAILED_PRECONDITION -> Status.FAILED_PRECONDITION
            Code.CODE_ABORTED -> Status.ABORTED
            Code.CODE_OUT_OF_RANGE -> Status.OUT_OF_RANGE
            Code.CODE_UNIMPLEMENTED -> Status.UNIMPLEMENTED
            Code.CODE_INTERNAL -> Status.INTERNAL
            Code.CODE_UNAVAILABLE -> Status.UNAVAILABLE
            Code.CODE_DATA_LOSS -> Status.DATA_LOSS
            Code.CODE_UNAUTHENTICATED -> Status.UNAUTHENTICATED
            Code.CODE_UNSPECIFIED, Code.UNRECOGNIZED -> Status.UNKNOWN
        }

    fun payload(
        data: ByteString = ByteString.EMPTY,
        requestInfo: ConformancePayload.RequestInfo? = null,
    ): ConformancePayload {
        val builder = ConformancePayload.newBuilder().setData(data)
        if (requestInfo != null) {
            builder.requestInfo = requestInfo
        }
        return builder.build()
    }
}
