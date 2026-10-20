package io.github.lldata.connect.conformance

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/** Big-endian fixed-32 length-prefixed protobuf framing used by connectconformance. */
object LengthPrefixedIo {
    fun <T : MessageLite> read(input: InputStream, parser: Parser<T>): T {
        val sizeBytes = input.readNBytes(4)
        require(sizeBytes.size == 4) { "EOF reading ServerCompat size preface" }
        val size = ByteBuffer.wrap(sizeBytes).int
        require(size >= 0) { "Negative message size: $size" }
        val body = input.readNBytes(size)
        require(body.size == size) { "EOF reading ServerCompat body (want $size, got ${body.size})" }
        return parser.parseFrom(body)
    }

    fun write(output: OutputStream, message: MessageLite) {
        val body = message.toByteArray()
        val preface = ByteBuffer.allocate(4).putInt(body.size).array()
        output.write(preface)
        output.write(body)
        output.flush()
    }
}
