package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.ByteBuffer

class IncrementalEnvelopeParserTest {

    @Test
    fun `feeds envelopes across chunk boundaries`() {
        val a = envelope("hello".toByteArray())
        val b = envelope("world".toByteArray())
        val wire = a + b
        val parser = IncrementalEnvelopeParser(
            IncrementalEnvelopeParser.Mode.CONNECT,
            maxInboundMessageSize = 1024,
        )
        val mid = 3
        val first = parser.feed(wire.copyOfRange(0, mid))
        assertTrue(first.isEmpty())
        val second = parser.feed(wire.copyOfRange(mid, wire.size))
        assertEquals(2, second.size)
        assertArrayEquals("hello".toByteArray(), second[0])
        assertArrayEquals("world".toByteArray(), second[1])
        assertTrue(parser.finish().isEmpty())
    }

    @Test
    fun `rejects envelope over max size with resource_exhausted`() {
        val parser = IncrementalEnvelopeParser(
            IncrementalEnvelopeParser.Mode.CONNECT,
            maxInboundMessageSize = 8,
        )
        val big = envelope(ByteArray(32) { 1 })
        val ex = assertThrows<ConnectException> {
            parser.feed(big)
        }
        assertEquals(ConnectCode.RESOURCE_EXHAUSTED, ex.code)
    }

    @Test
    fun `finish rejects truncated trailing bytes`() {
        val parser = IncrementalEnvelopeParser(
            IncrementalEnvelopeParser.Mode.CONNECT,
            maxInboundMessageSize = 1024,
        )
        parser.feed(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x05, 0x01, 0x02))
        val ex = assertThrows<ConnectException> { parser.finish() }
        assertEquals(ConnectCode.INVALID_ARGUMENT, ex.code)
    }

    private fun envelope(payload: ByteArray): ByteArray {
        val out = ByteArray(5 + payload.size)
        out[0] = 0
        ByteBuffer.wrap(out, 1, 4).putInt(payload.size)
        System.arraycopy(payload, 0, out, 5, payload.size)
        return out
    }
}
