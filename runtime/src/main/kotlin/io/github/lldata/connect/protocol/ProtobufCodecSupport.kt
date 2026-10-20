package io.github.lldata.connect.protocol

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.nio.charset.StandardCharsets

/**
 * Encode/decode protobuf [Message] values for unary Connect codecs.
 */
object ProtobufCodecSupport {

    private val jsonParser: JsonFormat.Parser = JsonFormat.parser().ignoringUnknownFields()
    private val jsonPrinter: JsonFormat.Printer = JsonFormat.printer().omittingInsignificantWhitespace()

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
            ConnectCodec.JSON -> jsonPrinter.print(message).toByteArray(StandardCharsets.UTF_8)
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
            jsonParser.merge(json, builder)
        }
        return builder.build() as T
    }

    private fun <T : Message> newBuilder(type: Class<T>): Message.Builder {
        val method = type.getMethod("newBuilder")
        return method.invoke(null) as Message.Builder
    }
}
