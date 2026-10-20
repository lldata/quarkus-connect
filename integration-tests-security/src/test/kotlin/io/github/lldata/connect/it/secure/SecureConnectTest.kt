package io.github.lldata.connect.it.secure

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * Opt-in Quarkus Security on Connect JSON (basic auth + `@RolesAllowed`).
 * This module alone depends on `quarkus-security`; core Connect stays slim.
 */
@QuarkusTest
class SecureConnectTest {

    private val alice = basic("alice", "alice")
    private val admin = basic("admin", "admin")

    @Test
    fun `connect WhoAmI with user role succeeds`() {
        given()
            .auth().preemptive().basic("alice", "alice")
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .body("{}")
            .`when`()
            .post("/secure.v1.SecureService/WhoAmI")
            .then()
            .statusCode(200)
            .body("subject", equalTo("alice"))
    }

    @Test
    fun `connect AdminOnly without auth is unauthenticated or permission denied`() {
        // Proactive basic may yield 401 unauthenticated; anonymous @RolesAllowed → denial.
        val code =
            given()
                .header("Content-Type", "application/json")
                .header("connect-protocol-version", "1")
                .body("{}")
                .`when`()
                .post("/secure.v1.SecureService/AdminOnly")
                .then()
                .extract()
                .statusCode()
        assert(code == 401 || code == 403) { "expected 401/403, got $code" }
    }

    @Test
    fun `connect AdminOnly as user is permission denied`() {
        given()
            .auth().preemptive().basic("alice", "alice")
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", alice)
            .body("{}")
            .`when`()
            .post("/secure.v1.SecureService/AdminOnly")
            .then()
            .statusCode(403)
            .header("Content-Type", equalTo("application/json"))
            .body("code", equalTo("permission_denied"))
    }

    @Test
    fun `connect AdminOnly as admin succeeds`() {
        given()
            .auth().preemptive().basic("admin", "admin")
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", admin)
            .body("{}")
            .`when`()
            .post("/secure.v1.SecureService/AdminOnly")
            .then()
            .statusCode(200)
            .body("subject", equalTo("admin"))
    }

    @Test
    fun `grpc-web WhoAmI with basic auth succeeds`() {
        // Empty protobuf WhoAmIRequest framed as gRPC-Web (flags=0, length=0).
        val frame = byteArrayOf(0, 0, 0, 0, 0)
        given()
            .auth().preemptive().basic("alice", "alice")
            .header("Content-Type", "application/grpc-web+proto")
            .header("Authorization", alice)
            .body(frame)
            .`when`()
            .post("/secure.v1.SecureService/WhoAmI")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/grpc-web+proto"))
    }

    private fun basic(user: String, pass: String): String =
        "Basic " + Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
}
