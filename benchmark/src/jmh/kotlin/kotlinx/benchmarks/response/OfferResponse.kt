package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class OfferResponse(
        val id: String,
        val activationActions: Set<ActivationAction>? = emptySet(),
        val purchasedSubscriptionId: String? = null,
        val description: String = "",
        val type: String = "",
        val duration: Long? = null,
        val subscriptions: ElementRelationListResponse? = null,
        val purchasesLeft: Int? = null,
        val purchasesNum: Int? = null,
        val renewalDurationDays: Int? = null,
        val renewalCount: Int? = null,
        val renewalPrice: PriceResponse? = null,
        val discountPrice: PriceResponse? = null,
        val discountRenewalPrice: PriceResponse? = null,
        val discountRenewalCount: Int? = null,
        val discountRenewalRemainingCount: Int? = null,
        val discountPercentage: Int? = null,
        val discountPricePercentage: Int? = null,
        val discountRenewalPricePercentage: Int? = null,
        val needCard: Boolean? = null,
        val endDate: Long? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class OfferListResponse(val items: List<OfferResponse> = emptyList())

@Serializable
@JsonClass(generateAdapter = true)
data class OfferActivationListResponse(
        val items: List<OfferActivationResponse>? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class OfferActivationResponse(
        val offerId: String,
        val statusCode: String,
        val status: Int,
        val startDate: Long? = null,
        val endDate: Long? = null,
        val failedReason: String? = null,
        val activatedOffer: OfferResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class ActivationAction(val activationActionType: ActivationActionType? = null)