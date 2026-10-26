package io.github.lldata.connect.protocol

import com.google.protobuf.Descriptors
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.JsonFormat.TypeRegistry
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.nio.charset.StandardCharsets

/**
 * Encode/decode protobuf [Message] values for the Connect and gRPC-Web codecs.
 *
 * @param preserveProtoFieldNames print JSON with the proto field names (`request_id`) instead of
 *   the proto3 JSON lowerCamelCase names (`requestId`). Parsing accepts both either way.
 */
class ProtobufCodecSupport(preserveProtoFieldNames: Boolean = false) {

    private val baseJsonParser: JsonFormat.Parser = JsonFormat.parser().ignoringUnknownFields()

    // Always print fields (including defaults) so clients see int64 "0" and bool false —
    // clearer for resume offsets and flags over Connect JSON.
    private val baseJsonPrinter: JsonFormat.Printer = JsonFormat.printer()
        .omittingInsignificantWhitespace()
        .includingDefaultValueFields()
        .let { if (preserveProtoFieldNames) it.preservingProtoFieldNames() else it }

    fun <T : Message> parse(bytes: ByteArray, type: Class<T>, codec: ConnectCodec): T {
        return try {
            when (codec) {
                ConnectCodec.PROTO -> parseProto(bytes, type)
                ConnectCodec.JSON -> parseJson(bytes, type)
            }
        } catch (e: InvalidProtocolBufferException) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid request payload: ${e.message}", e)
        } catch (e: ConnectException) {
            throw e
        } catch (e: Exception) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid request payload: ${e.message}", e)
        }
    }

    fun encode(message: Message, codec: ConnectCodec): ByteArray {
        return when (codec) {
            ConnectCodec.PROTO -> message.toByteArray()
            // TypeRegistry covers google.protobuf.Any payloads (e.g. conformance RequestInfo).
            ConnectCodec.JSON ->
                baseJsonPrinter.usingTypeRegistry(registryFor(message.descriptorForType.file))
                    .print(message)
                    .toByteArray(StandardCharsets.UTF_8)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Message> parseProto(bytes: ByteArray, type: Class<T>): T {
        val builder = newBuilder(type)
        builder.mergeFrom(bytes)
        return builder.build() as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Message> parseJson(bytes: ByteArray, type: Class<T>): T {
        val builder = newBuilder(type)
        val json = String(bytes, StandardCharsets.UTF_8)
        if (json.isNotBlank()) {
            baseJsonParser.usingTypeRegistry(registryFor(builder.descriptorForType.file))
                .merge(json, builder)
        }
        return builder.build() as T
    }

    private fun <T : Message> newBuilder(type: Class<T>): Message.Builder {
        val method = type.getMethod("newBuilder")
        return method.invoke(null) as Message.Builder
    }

    /** Register every message in [file] (and dependencies) so JsonFormat can expand Any. */
    private fun registryFor(file: Descriptors.FileDescriptor): TypeRegistry {
        val builder = TypeRegistry.newBuilder()
        val seen = HashSet<String>()
        fun addFile(f: Descriptors.FileDescriptor) {
            if (!seen.add(f.fullName)) return
            fun addMessage(m: Descriptors.Descriptor) {
                builder.add(m)
                m.nestedTypes.forEach { addMessage(it) }
            }
            f.messageTypes.forEach { addMessage(it) }
            f.dependencies.forEach { addFile(it) }
            f.publicDependencies.forEach { addFile(it) }
        }
        addFile(file)
        return builder.build()
    }
}
