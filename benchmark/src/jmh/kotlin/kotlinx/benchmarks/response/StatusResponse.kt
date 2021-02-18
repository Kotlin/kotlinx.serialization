package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class StatusResponse(
        val callId: String? = null,
        val status: Int = 0,
        val serviceInfo: ServiceInfoHolderResponse? = null,
        val userInfo: UserInfoResponse? = null,
        val uiScreenInfo: UiScreenInfoResponse? = null,
        val offerActivations: OfferActivationListResponse? = null
) {

}

@Serializable
@JsonClass(generateAdapter = true)
data class ServiceInfoHolderResponse(
        val serverTime: Long? = null
)