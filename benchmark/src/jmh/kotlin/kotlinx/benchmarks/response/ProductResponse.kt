package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class ProductListResponse(
        val items: List<ProductResponse> = emptyList(),
        val emptyReason: EmptyReason? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class ProductResponse(
        val id: String? = null,
        val type: String? = null,
        val duration: Long? = null,
        val subscriptionPeriod: Long? = null,
        val consumptionMode: ConsumptionMode? = null,
        val qualities: List<String>? = emptyList(),
        val price: PriceResponse? = null,
        val originalPrice: PriceResponse? = null,
        val noSalePrice: Double? = null,
        val wholesale: Int? = null,
        val priceCategory: String? = null,
        val paymentMethods: PaymentMethodListResponse? = null,
        val offer: OfferResponse? = null,
        val subscription: ElementRelationResponse? = null,
        val audioTracks: AudioTrackListResponse? = null,
        val subtitles: SubtitleListResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class PriceResponse(
        val value: Double,
        val currencyCode: String
)