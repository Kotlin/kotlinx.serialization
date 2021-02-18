package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class UiScreenInfoResponse(
        val attributes: Map<String, String>? = null
)
