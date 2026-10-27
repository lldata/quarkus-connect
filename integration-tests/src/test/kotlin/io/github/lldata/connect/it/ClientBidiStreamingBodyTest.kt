package io.github.lldata.connect.it

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * Client-streaming request bodies are parsed incrementally: many small envelopes whose
 * total size exceeds [quarkus.connect.max-inbound-message-size] still succeed, while a
 * single oversize envelope is refused with `resource_exhausted`.
 */
@QuarkusTest
@TestProfile(ClientBidiStreamingBodyTest.Profile::class)
class ClientBidiStreamingBodyTest {

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> =
            mapOf(
                "quarkus.connect.max-inbound-message-size" to "256",
                "quarkus.http.limits.max-body-size" to "8M",
            )
    }

    private val auth = "Bearer ${BearerAuthInterceptor.VALID_TOKEN}"

    @Test
    fun `many small envelopes totaling more than max message size succeed`() {
        // 40 envelopes × ~20-byte JSON ≈ 800+ bytes wire > 256 max message size.
        val envelopes = (1..40).map { i ->
            envelope("""{"name":"n$i"}""".toByteArray())
        }
        val body = envelopes.fold(ByteArray(0)) { acc, e -> acc + e }
        assertTrue(body.size > 256, "body ${body.size} should exceed max message size")

        given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(body)
            .`when`()
            .post("/greet.v1.GreetService/CollectGreetings")
            .then()
            .statusCode(200)
            .body(containsString("Hello"))
    }

    @Test
    fun `single envelope over max message size is resource_exhausted`() {
        val payload = """{"name":"${"z".repeat(300)}"}""".toByteArray()
        assertTrue(payload.size > 256)

        given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(envelope(payload))
            .`when`()
            .post("/greet.v1.GreetService/CollectGreetings")
            .then()
            .statusCode(200)
            .body(containsString("\"code\":\"resource_exhausted\""))
            .body(containsString("exceeds limit 256"))
    }

    @Test
    fun `bidi many small envelopes totaling more than max still stream`() {
        val envelopes = (1..30).map { i ->
            envelope("""{"name":"b$i"}""".toByteArray())
        }
        val body = envelopes.fold(ByteArray(0)) { acc, e -> acc + e }
        assertTrue(body.size > 256)

        val raw = given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(body)
            .`when`()
            .post("/greet.v1.GreetService/ChatGreetings")
            .then()
            .statusCode(200)
            .extract()
            .asByteArray()

        // End-stream flag bit1 set somewhere; response should not be an immediate error-only body.
        assertTrue(raw.size > 5)
        // At least one data envelope greeting.
        val text = String(raw)
        assertTrue(text.contains("Hello") || text.contains("greeting"), text.take(200))
    }

    private fun envelope(payload: ByteArray): ByteArray {
        val out = ByteArray(5 + payload.size)
        ByteBuffer.wrap(out, 1, 4).putInt(payload.size)
        System.arraycopy(payload, 0, out, 5, payload.size)
        return out
    }
}
