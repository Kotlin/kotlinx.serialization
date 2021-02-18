package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PlaybackResponse(
        val callId: String,
        val playbackState: PlaybackState? = null
)