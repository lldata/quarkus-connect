package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectErrorDetail
import io.github.lldata.connect.spi.ConnectException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * Serialize Connect unary errors to `application/json` bodies per the protocol reference.
 */
object ConnectErrorJson {

    fun toJson(error: ConnectException): String {
        val obj = JsonObject().put("code", error.code.wireName)
        val message = error.message
        if (!message.isNullOrEmpty()) {
            obj.put("message", message)
        }
        if (error.details.isNotEmpty()) {
            obj.put("details", detailsArray(error.details))
        }
        return obj.encode()
    }

    private fun detailsArray(details: List<ConnectErrorDetail>): JsonArray {
        val arr = JsonArray()
        for (detail in details) {
            val item = JsonObject()
                .put("type", detail.type)
                .put("value", detail.valueBase64)
            if (detail.debug != null) {
                item.put("debug", detail.debug)
            }
            arr.add(item)
        }
        return arr
    }
}
