package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class ElementRelationResponse(
        val element: ElementResponse,
        val uiType: UiType? = null
)