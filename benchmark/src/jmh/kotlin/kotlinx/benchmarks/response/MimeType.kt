package response

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MimeType(val type: String) {

    @SerialName("mp4/sbrmp4")
    @Json(name = "mp4/sbrmp4")
    MP4("mp4/sbrmp4"),

    @SerialName("mp4/sbrwvm")
    @Json(name = "mp4/sbrwvm")
    WVM_OFFLINE("mp4/sbrwvm"),

    @SerialName("mp4/ts")
    @Json(name = "mp4/ts")
    MP4_TS("mp4/ts"),

    @SerialName("video/vnd.ms-playready.media.ism")
    @Json(name = "video/vnd.ms-playready.media.ism")
    PLAYREADY_SS("video/vnd.ms-playready.media.ism"),

    @SerialName("application/dash+xml+hdr")
    @Json(name = "application/dash+xml+hdr")
    CENC_DASH_HDR("application/dash+xml+hdr"),

    @SerialName("application/dash+xml")
    @Json(name = "application/dash+xml")
    CENC_DASH_ONLINE("application/dash+xml"),

    @SerialName("application/dash+xml+offline")
    @Json(name = "application/dash+xml+offline")
    CENC_DASH_OFFLINE("application/dash+xml+offline"),

    @SerialName("audio/embedded+aac")
    @Json(name = "audio/embedded+aac")
    AUDIO_AAC("audio/embedded+aac"),

    @SerialName("audio/embedded+eac3")
    @Json(name = "audio/embedded+eac3")
    AUDIO_EAC3("audio/embedded+eac3"),

    @SerialName("audio/embedded+eac3+atmos")
    @Json(name = "audio/embedded+eac3+atmos")
    AUDIO_EAC3_ATMOS("audio/embedded+eac3+atmos"),

    @SerialName("audio/mpeg")
    @Json(name = "audio/mpeg")
    AUDIO_MPEG("audio/mpeg"),

    @SerialName("audio/mp4")
    @Json(name = "audio/mp4")
    AUDIO_MP4("audio/mp4"),

    @SerialName("application/x-smil")
    @Json(name = "application/x-smil")
    SUB_SMI("application/x-smil"),

    @SerialName("application/x-subrip")
    @Json(name = "application/x-subrip")
    SUB_SRT("application/x-subrip"),

    @SerialName("subs/embedded+srt")
    @Json(name = "subs/embedded+srt")
    SUB_EMBEDDED_SRT("subs/embedded+srt"),

    @SerialName("")
    @Json(name = "")
    UNKNOWN("");

}