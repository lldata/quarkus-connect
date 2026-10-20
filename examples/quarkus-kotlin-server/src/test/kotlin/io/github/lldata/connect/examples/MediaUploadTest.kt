package io.github.lldata.connect.examples

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

@QuarkusTest
class MediaUploadTest {

    private val auth = "Bearer demo-user"

    @Test
    fun `chunked upload resume and idempotent complete with range playback`() {
        val clientRequestId = UUID.randomUUID().toString()
        // ~1.5MB multi-chunk proof (same path as 100MB+; full EDGE soak remains manual).
        val payload = ByteArray(1_500_000) { (it % 251).toByte() }
        val chunkSize = 64_000

        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(
                """
                {
                  "clientRequestId":"$clientRequestId",
                  "filename":"clip.bin",
                  "contentType":"application/octet-stream",
                  "totalSizeBytes":"${payload.size}",
                  "kind":"MEDIA_KIND_VIDEO"
                }
                """.trimIndent(),
            )
            .`when`()
            .post("/media.v1.MediaService/BeginUpload")
            .then()
            .statusCode(200)
            // proto3 JSON omits default scalars (0 / false)
            .body("uploadId", org.hamcrest.Matchers.notNullValue())
            .body("clientRequestId", equalTo(clientRequestId))

        var offset = 0
        var chunkIndex = 0
        while (offset < payload.size) {
            val end = minOf(offset + chunkSize, payload.size)
            val chunk = payload.copyOfRange(offset, end)
            val b64 = Base64.getEncoder().encodeToString(chunk)
            given()
                .header("Content-Type", "application/json")
                .header("connect-protocol-version", "1")
                .header("Authorization", auth)
                .body(
                    """
                    {
                      "clientRequestId":"$clientRequestId",
                      "offset":"$offset",
                      "chunkIndex":$chunkIndex,
                      "data":"$b64"
                    }
                    """.trimIndent(),
                )
                .`when`()
                .post("/media.v1.MediaService/UploadChunk")
                .then()
                .statusCode(200)
                .body("nextOffset", equalTo(end.toString()))

            // Idempotent retry of the same chunk
            if (chunkIndex == 0) {
                given()
                    .header("Content-Type", "application/json")
                    .header("connect-protocol-version", "1")
                    .header("Authorization", auth)
                    .body(
                        """
                        {
                          "clientRequestId":"$clientRequestId",
                          "offset":"$offset",
                          "chunkIndex":$chunkIndex,
                          "data":"$b64"
                        }
                        """.trimIndent(),
                    )
                    .`when`()
                    .post("/media.v1.MediaService/UploadChunk")
                    .then()
                    .statusCode(200)
                    .body("nextOffset", equalTo(end.toString()))
            }

            offset = end
            chunkIndex++
        }

        // Resume status after "drop"
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"clientRequestId":"$clientRequestId"}""")
            .`when`()
            .post("/media.v1.MediaService/GetUploadStatus")
            .then()
            .statusCode(200)
            .body("bytesReceived", equalTo(payload.size.toString()))
            .body("completed", equalTo(false))

        val messageId = given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(
                """
                {
                  "clientRequestId":"$clientRequestId",
                  "durationMs":"12345",
                  "posterJpeg":""
                }
                """.trimIndent(),
            )
            .`when`()
            .post("/media.v1.MediaService/CompleteUpload")
            .then()
            .statusCode(200)
            .body("message.durationMs", equalTo("12345"))
            .body("message.sizeBytes", equalTo(payload.size.toString()))
            .extract()
            .path<String>("message.messageId")

        // Idempotent complete
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"clientRequestId":"$clientRequestId","durationMs":"999"}""")
            .`when`()
            .post("/media.v1.MediaService/CompleteUpload")
            .then()
            .statusCode(200)
            .body("message.messageId", equalTo(messageId))
            .body("message.durationMs", equalTo("12345"))

        // HTTP range seek
        val ranged = given()
            .header("Range", "bytes=0-99")
            .`when`()
            .get("/media/$messageId")
            .then()
            .statusCode(206)
            .header("Content-Range", equalTo("bytes 0-99/${payload.size}"))
            .extract()
            .asByteArray()
        assertEquals(100, ranged.size)
        assertArrayEquals(payload.copyOfRange(0, 100), ranged)

        // Poster
        given()
            .`when`()
            .get("/media/$messageId/poster")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("image/jpeg"))

        // Polling list
        given()
            .header("Content-Type", "application/json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body("""{"afterSequence":"0","limit":10}""")
            .`when`()
            .post("/media.v1.MediaService/ListMessages")
            .then()
            .statusCode(200)
            .body("messages.size()", greaterThanOrEqualTo(1))

        // Connect server streaming catch-up (WatchMessages)
        val watchJson = """{"afterSequence":"0","limit":10}""".toByteArray()
        val watchReq = ByteArray(5 + watchJson.size)
        watchReq[0] = 0x00
        watchReq[1] = ((watchJson.size ushr 24) and 0xff).toByte()
        watchReq[2] = ((watchJson.size ushr 16) and 0xff).toByte()
        watchReq[3] = ((watchJson.size ushr 8) and 0xff).toByte()
        watchReq[4] = (watchJson.size and 0xff).toByte()
        System.arraycopy(watchJson, 0, watchReq, 5, watchJson.size)

        val streamRaw = given()
            .header("Content-Type", "application/connect+json")
            .header("connect-protocol-version", "1")
            .header("Authorization", auth)
            .body(watchReq)
            .`when`()
            .post("/media.v1.MediaService/WatchMessages")
            .then()
            .statusCode(200)
            .header("Content-Type", equalTo("application/connect+json"))
            .extract()
            .asByteArray()

        var offsetStream = 0
        var sawEnd = false
        var sawMessage = false
        while (offsetStream + 5 <= streamRaw.size) {
            val flag = streamRaw[offsetStream].toInt() and 0xff
            val len = ByteBuffer.wrap(streamRaw, offsetStream + 1, 4).int
            val payload = String(streamRaw, offsetStream + 5, len)
            offsetStream += 5 + len
            if (flag and 0x02 != 0) {
                sawEnd = true
            } else if (payload.contains(messageId)) {
                sawMessage = true
            }
        }
        assertTrue(sawEnd)
        assertTrue(sawMessage)
    }
}
