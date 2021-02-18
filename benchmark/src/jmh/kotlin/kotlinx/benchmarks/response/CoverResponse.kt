package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class CoverElementHolderResponse(
        val items: List<CoverElementResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class CoverElementResponse(
        val url: String,
        val imageType: String? = null,
        val width: Int? = null,
        val height: Int? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class BasicCoverElementHolderResponse(
        val items: List<BasicCoverElementResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class BasicCoverElementResponse(
        val url: String,
        val imageType: String,
        val primaryColor: String? = null
)