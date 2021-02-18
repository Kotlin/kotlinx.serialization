package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import response.PriceResponse

@Serializable
@JsonClass(generateAdapter = true)
data class StickerResponse(
        val type: String,
        val price: PriceResponse? = null,
        val priceMultiplier: Double? = null,
        val period: Long? = null,
        val subscriptionName: String? = null
)