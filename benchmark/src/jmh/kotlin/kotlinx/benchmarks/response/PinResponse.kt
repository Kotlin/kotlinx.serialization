package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PinResponse(val pinInfo: PinInfoResponse)

@Serializable
@JsonClass(generateAdapter = true)
data class PinInfoResponse(val pin: String)