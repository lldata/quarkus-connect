package io.github.lldata.connect.it

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * `quarkus.connect.json.preserve-proto-field-names=true`: JSON responses use the proto field
 * names (`request_id`) on Connect unary, Connect streaming and gRPC-Web `+json`.
 * The default (lowerCamel `requestId`) is covered by [GreetConnectTest].
 */
@QuarkusTest
@TestProfile(PreserveProtoFieldNamesTest.Profile::class)
class PreserveProtoFieldNamesTest {

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> =
            mapOf("quarkus.connect.json.preserve-proto-field-names" to "true")
    }

    private val auth = "Bearer ${BearerAuthInterceptor.VALID_TOKEN}"

    @Test
    fun `connect JSON unary response keeps proto field names`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"Buf","request_id":"42"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/json"))
            .body("greeting", equalTo("Hello, Buf!"))
            .body("request_id", equalTo("42"))
            .body("$", not(hasKey("requestId")))
    }

    @Test
    fun `lowerCamel requests are still accepted`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"Camel","requestId":"7"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .body("request_id", equalTo("7"))
    }

    @Test
    fun `connect server streaming messages keep proto field names`() {
        val raw = given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(envelope("""{"name":"Stream","request_id":"5"}""".toByteArray()))
            .`when`()
            .post("/greet.v1.GreetService/StreamGreetings")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/connect+json"))
            .extract()
            .asByteArray()

        val messages = mutableListOf<String>()
        var offset = 0
        while (offset + 5 <= raw.size) {
            val flag = raw[offset].toInt() and 0xff
            val len = ByteBuffer.wrap(raw, offset + 1, 4).int
            val payload = String(raw, offset + 5, len)
            offset += 5 + len
            if (flag and 0x02 == 0) messages += payload
        }
        assertEquals(3, messages.size)
        for (message in messages) {
            assertTrue(message.contains("\"request_id\":\"5\""), message)
            assertFalse(message.contains("requestId"), message)
        }
    }

    @Test
    fun `grpc-web JSON response keeps proto field names`() {
        val raw = given()
            .header("Content-Type", "application/grpc-web+json")
            .header("Authorization", auth)
            .body(envelope("""{"name":"Web","request_id":"3"}""".toByteArray()))
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .extract()
            .asByteArray()

        assertEquals(0, raw[0].toInt() and 0xff) // data frame
        val len = ByteBuffer.wrap(raw, 1, 4).int
        val message = String(raw, 5, len)
        assertTrue(message.contains("\"request_id\":\"3\""), message)
        assertFalse(message.contains("requestId"), message)
    }

    private fun envelope(payload: ByteArray): ByteArray {
        val out = ByteArray(5 + payload.size)
        ByteBuffer.wrap(out, 1, 4).putInt(payload.size)
        System.arraycopy(payload, 0, out, 5, payload.size)
        return out
    }
}
