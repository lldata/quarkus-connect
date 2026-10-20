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
            .queryParam("connect", "v1")
            .`when`()
            .get("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Vary", equalTo("Accept-Encoding"))
            .body("greeting", equalTo("Hello, Get!"))
    }

    @Test
    fun `GET unary with URL-safe base64 proto message`() {
        val request = GreetRequest.newBuilder().setName("ProtoGet").setRequestId(9).build()
        val b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(request.toByteArray())

        val raw = given()
            .header("Authorization", auth)
            .queryParam("connect", "v1")
            .queryParam("encoding", "proto")
            .queryParam("base64", "1")
            .queryParam("message", b64)
            .`when`()
            .get("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/proto"))
            .header("Vary", equalTo("Accept-Encoding"))
            .extract()
            .asByteArray()

        val response = GreetResponse.parseFrom(raw)
        assertEquals("Hello, ProtoGet!", response.greeting)
        assertEquals(9L, response.requestId)
    }

    @Test
    fun `unsupported connect-protocol-version returns invalid_argument`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "99")
            .header("Authorization", auth)
            .body("""{"name":"Buf"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(400)
            .body("code", equalTo("invalid_argument"))
    }

    @Test
    fun `Java SPI Ping sets trailer-prefixed metadata`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"name":"Spi"}""")
            .`when`()
            .post("/greet.v1.GreetService/Ping")
            .then()
            .statusCode(200)
            .header("x-ping", equalTo("1"))
            .header("trailer-x-ping-trail", equalTo("ok"))
            .body("greeting", equalTo("Pong, Spi!"))
    }

    @Test
    fun `error JSON omits null message field`() {
        // Ping SPI throws ConnectException(NOT_FOUND, null) for name=nomsg
        val body = given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .body("""{"name":"nomsg"}""")
            .`when`()
            .post("/greet.v1.GreetService/Ping")
            .then()
            .statusCode(404)
            .body("code", equalTo("not_found"))
            .extract()
            .asString()
        assertTrue(!body.contains("\"message\""), "error JSON must omit empty message: $body")
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
    fun `connect-timeout-ms yields deadline_exceeded`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("connect-timeout-ms", "200")
            .header("Authorization", auth)
            .body("""{"name":"slow"}""")
            .`when`()
            .post("/greet.v1.GreetService/Slow")
            .then()
            .statusCode(504)
            .body("code", equalTo("deadline_exceeded"))
    }

    @Test
    fun `google rpc ErrorInfo details appear in Connect JSON`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"code":"invalid_argument","message":"bad field"}""")
            .`when`()
            .post("/greet.v1.GreetService/FailWithDetails")
            .then()
            .statusCode(400)
            .body("code", equalTo("invalid_argument"))
            .body("message", equalTo("bad field"))
            .body("details[0].type", equalTo("google.rpc.ErrorInfo"))
            .body("details[0].value", org.hamcrest.Matchers.notNullValue())
    }

    @Test
    fun `connect server streaming envelopes`() {
        // Request: one Connect envelope with JSON payload {"name":"Stream"}
        val json = """{"name":"Stream"}""".toByteArray()
        val req = ByteArray(5 + json.size)
        req[0] = 0x00
        req[1] = ((json.size ushr 24) and 0xff).toByte()
        req[2] = ((json.size ushr 16) and 0xff).toByte()
        req[3] = ((json.size ushr 8) and 0xff).toByte()
        req[4] = (json.size and 0xff).toByte()
        System.arraycopy(json, 0, req, 5, json.size)

        val raw = given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(req)
            .`when`()
            .post("/greet.v1.GreetService/StreamGreetings")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/connect+json"))
            .extract()
            .asByteArray()

        val messages = mutableListOf<String>()
        var offset = 0
        var sawEnd = false
        while (offset + 5 <= raw.size) {
            val flag = raw[offset].toInt() and 0xff
            val len = ByteBuffer.wrap(raw, offset + 1, 4).int
            val payload = String(raw, offset + 5, len)
            offset += 5 + len
            if (flag and 0x02 != 0) {
                sawEnd = true
                assertTrue(payload.contains("{}") || payload.contains("metadata") || !payload.contains("\"error\""))
            } else {
                messages += payload
            }
        }
        assertTrue(sawEnd)
        assertEquals(3, messages.size)
        assertTrue(messages[0].contains("Hello #1, Stream!"))
        assertTrue(messages[2].contains("Hello #3, Stream!"))
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
