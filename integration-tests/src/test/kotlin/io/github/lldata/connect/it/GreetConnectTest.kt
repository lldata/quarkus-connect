package io.github.lldata.connect.it

import io.github.lldata.connect.it.greet.v1.GreetRequest
import io.github.lldata.connect.it.greet.v1.GreetResponse
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

@QuarkusTest
class GreetConnectTest {

    private val auth = "Bearer ${BearerAuthInterceptor.VALID_TOKEN}"

    @Test
    fun `connect JSON unary via @GrpcService`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"Buf","requestId":"42"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/json"))
            .body("greeting", equalTo("Hello, Buf!"))
            .body("requestId", equalTo("42"))
    }

    @Test
    fun `connect protobuf unary via @GrpcService`() {
        val request = GreetRequest.newBuilder().setName("Connect").setRequestId(7).build()

        val raw = given()
            .header("Content-Type", "application/proto")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(request.toByteArray())
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/proto"))
            .extract()
            .asByteArray()

        val response = GreetResponse.parseFrom(raw)
        assertEquals("Hello, Connect!", response.greeting)
        assertEquals(7L, response.requestId)
    }

    @Test
    fun `missing bearer maps to unauthenticated Connect error`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .body("""{"name":"Buf"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(401)
            .header("Content-Type", equalTo("application/json"))
            .body("code", equalTo("unauthenticated"))
    }

    @Test
    fun `permission denied from service maps to Connect error`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"forbidden"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(403)
            .body("code", equalTo("permission_denied"))
    }

    @Test
    fun `invalid argument maps to Connect JSON error`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":""}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(400)
            .header("Content-Type", equalTo("application/json"))
            .body("code", equalTo("invalid_argument"))
    }

    @Test
    fun `status mapping for Fail RPC`() {
        val cases = listOf(
            "not_found" to 404,
            "already_exists" to 409,
            "failed_precondition" to 400,
            "resource_exhausted" to 429,
            "unimplemented" to 501,
        )
        for ((code, http) in cases) {
            given()
                .header("Content-Type", "application/json")
                .header("connect-protocol-version", "1")
                .header("Authorization", auth)
                .body("""{"code":"$code","message":"demo $code"}""")
                .`when`()
                .post("/greet.v1.GreetService/Fail")
                .then()
                .statusCode(http)
                .body("code", equalTo(code))
                .body("message", equalTo("demo $code"))
        }
    }

    @Test
    fun `unsupported content type returns 415`() {
        given()
            .header("Content-Type", "text/plain")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("name")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(415)
            .header("Content-Type", equalTo("application/json"))
            .body("code", equalTo("invalid_argument"))
    }

    @Test
    fun `GET unary with JSON message query`() {
        given()
            .header("Authorization", auth)
            .queryParam("encoding", "json")
            .queryParam("message", """{"name":"Get"}""")
            .queryParam("connect", "1")
            .`when`()
            .get("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .body("greeting", equalTo("Hello, Get!"))
    }

    @Test
    fun `grpc-web unary protobuf`() {
        val request = GreetRequest.newBuilder().setName("Web").setRequestId(1).build()
        val framed = grpcWebDataFrame(request.toByteArray())

        val raw = given()
            .header("Content-Type", "application/grpc-web+proto")
            .header("Authorization", auth)
            .body(framed)
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", startsWith("application/grpc-web"))
            .extract()
            .asByteArray()

        assertTrue(raw.size >= 5)
        assertEquals(0, raw[0].toInt() and 0xff) // data frame
        val len = ByteBuffer.wrap(raw, 1, 4).int
        val message = GreetResponse.parseFrom(raw.copyOfRange(5, 5 + len))
        assertEquals("Hello, Web!", message.greeting)

        // Trailer frame with grpc-status: 0
        val trailerOffset = 5 + len
        assertTrue(raw.size > trailerOffset + 5)
        assertEquals(0x80, raw[trailerOffset].toInt() and 0xff)
        val trailerText = String(raw, trailerOffset + 5, raw.size - trailerOffset - 5)
        assertTrue(trailerText.contains("grpc-status: 0"))
    }

    @Test
    fun `grpc-web unauthenticated`() {
        val request = GreetRequest.newBuilder().setName("Web").build()
        val framed = grpcWebDataFrame(request.toByteArray())

        val raw = given()
            .header("Content-Type", "application/grpc-web+proto")
            .body(framed)
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .extract()
            .asByteArray()

        assertEquals(0x80, raw[0].toInt() and 0xff) // trailers-only
        val trailerText = String(raw, 5, raw.size - 5)
        assertTrue(trailerText.contains("grpc-status: 16")) // UNAUTHENTICATED
    }

    private fun grpcWebDataFrame(payload: ByteArray): ByteArray {
        val out = ByteArray(5 + payload.size)
        out[0] = 0x00
        out[1] = ((payload.size ushr 24) and 0xff).toByte()
        out[2] = ((payload.size ushr 16) and 0xff).toByte()
        out[3] = ((payload.size ushr 8) and 0xff).toByte()
        out[4] = (payload.size and 0xff).toByte()
        System.arraycopy(payload, 0, out, 5, payload.size)
        return out
    }
}
