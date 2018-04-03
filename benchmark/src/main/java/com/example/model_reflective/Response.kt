package com.example.model_reflective

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON

@Serializable
class Response {

    var users: List<User>? = null

    var status: String? = null

    @SerialName("is_real_json")
    @SerializedName("is_real_json") // Annotation needed for GSON
    @Json(name = "is_real_json")
    var isRealJson: Boolean = false

    fun stringify(): String {
        return JSON.stringify(this)
    }

    companion object {
        @JvmStatic
        fun parse(str: String): Response {
            return JSON.Companion.parse(str)
        }
    }
}
