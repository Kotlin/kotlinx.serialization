package response

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AudioTrackChannel {

    @SerialName("2.0")
    @Json(name = "2.0")
    STEREO,

    @SerialName("5.1")
    @Json(name = "5.1")
    DOLBY_DIGITAL

}