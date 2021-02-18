package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class ElementRelationListResponse(
        val items: List<ElementRelationResponse>? = null,
        val totalSize: Int? = null
)