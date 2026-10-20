package io.github.lldata.connect.it

import io.github.lldata.connect.it.greet.v1.GreetRequest
import io.github.lldata.connect.it.greet.v1.GreetResponse
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetConnectTest {

    @Test
    fun `unary greet with JSON`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .body("""{"name":"Buf"}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/json"))
            .header("x-quarkus-connect", equalTo("v1"))
            .body("greeting", equalTo("Hello, Buf!"))
    }

    @Test
    fun `unary greet with protobuf`() {
        val request = GreetRequest.newBuilder().setName("Connect").build()

        val raw = given()
            .header("Content-Type", "application/proto")
            .header("connect-protocol-version", "1")
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
    }

    @Test
    fun `invalid argument maps to Connect JSON error`() {
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .body("""{"name":""}""")
            .`when`()
            .post("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(400)
            .header("Content-Type", equalTo("application/json"))
            .body("code", equalTo("invalid_argument"))
    }

    @Test
    fun `unsupported content type returns 415`() {
        given()
            .header("Content-Type", "text/plain")
            .header("connect-protocol-version", "1")
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
            .queryParam("encoding", "json")
            .queryParam("message", """{"name":"Get"}""")
            .queryParam("connect", "1")
            .`when`()
            .get("/greet.v1.GreetService/Greet")
            .then()
            .statusCode(200)
            .body("greeting", equalTo("Hello, Get!"))
    }
}
