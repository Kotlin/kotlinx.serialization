package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class ServiceInfoResponse(
        val notifications: NotificationItemsHolder? = null,
        val serverTime: Long? = null,
        val clientLocation: LocationResponse? = null
)