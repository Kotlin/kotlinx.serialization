package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PlaybackState(
        val errorCode: Int,
        val type: String? = null
)