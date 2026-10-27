package io.github.lldata.connect.it

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * `quarkus.connect.max-inbound-message-size` rejects oversized unary bodies and streaming
 * envelopes with Connect `resource_exhausted` (not a bare HTTP 413).
 */
@QuarkusTest
@TestProfile(MaxInboundMessageSizeTest.Profile::class)
class MaxInboundMessageSizeTest {

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> =
            mapOf(
                "quarkus.connect.max-inbound-message-size" to "64",
                // Ensure Vert.x does not 413 before Connect sees the body.
                "quarkus.http.limits.max-body-size" to "1M",
            )
    }

    private val auth = "Bearer ${BearerAuthInterceptor.VALID_TOKEN}"

    @Test
    fun `connect JSON unary over limit is resource_exhausted`() {
        // Proto JSON {"name":"..."} — pad name so decoded body > 64 bytes.
        val name = "x".repeat(80)
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"$name"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(429)
            .body("code", equalTo("resource_exhausted"))
            .body("message", containsString("exceeds limit 64"))
    }

    @Test
    fun `connect streaming envelope over limit is resource_exhausted`() {
        val payload = """{"name":"${"y".repeat(80)}"}""".toByteArray()
        val envelope = ByteArray(5 + payload.size)
        ByteBuffer.wrap(envelope, 1, 4).putInt(payload.size)
        System.arraycopy(payload, 0, envelope, 5, payload.size)

        given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(envelope)
            .`when`()
            .post("/greet.v1.GreetService/CollectGreetings")
            .then()
            .statusCode(200) // Connect streams use HTTP 200 + end-stream error
            .body(containsString("\"code\":\"resource_exhausted\""))
    }

    @Test
    fun `connect JSON unary under limit still succeeds`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"ok"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .body("greeting", equalTo("Hello, ok!"))
    }
}
