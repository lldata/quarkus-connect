package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCallContext
import io.grpc.Metadata
import java.util.Base64

/** Copy all values for each metadata key (grpc-java `get` returns only the last). */
internal object GrpcMetadataCopy {
    fun headersToContext(headers: Metadata, context: ConnectCallContext) {
        for (key in headers.keys()) {
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                val metaKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER)
                val values = headers.getAll(metaKey) ?: continue
                for (value in values) {
                    // gRPC-Web / Connect wire: binary metadata is unpadded base64.
                    context.responseHeader(key, Base64.getEncoder().withoutPadding().encodeToString(value))
                }
            } else {
                val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                val values = headers.getAll(metaKey) ?: continue
                for (value in values) {
                    context.responseHeader(key, value)
                }
            }
        }
    }

    fun trailersToContext(trailers: Metadata, context: ConnectCallContext) {
        for (key in trailers.keys()) {
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                val metaKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER)
                val values = trailers.getAll(metaKey) ?: continue
                for (value in values) {
                    context.responseTrailer(key, Base64.getEncoder().withoutPadding().encodeToString(value))
                }
            } else {
                val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                val values = trailers.getAll(metaKey) ?: continue
                for (value in values) {
                    context.responseTrailer(key, value)
                }
            }
        }
    }
}
