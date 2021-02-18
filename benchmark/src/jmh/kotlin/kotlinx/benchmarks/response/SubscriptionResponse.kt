package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class SubscriptionListHolderResponse(
        val serviceInfo: ServiceInfoResponse? = null,
        val userInfo: UserInfoResponse,
        val elements: SubscriptionListResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubscriptionListResponse(
        val items: List<ElementResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubscriptionHolderListResponse(
        val items: List<SubscriptionHolderResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubscriptionHolderResponse(
        val element: SubscriptionResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
        val id: String,
        val trailers: TrailerListResponse? = null,
        val title: String? = null,
        val description: String? = null,
        val type: String? = null,
        val expireDate: Long? = null,
        val fullSeasonPriceText: String? = null,
        val seasonSubscriptionEndDate: Long? = null,
        val renewable: Boolean? = null,
        val name: String? = null,
        val alias: String? = null,
        val storeType: String? = null,
        val covers: CoverElementHolderResponse? = null,
        val basicCovers: BasicCoverElementHolderResponse? = null,
        val duration: Long? = null,
        val subscriptionBoughtWithOffer: Boolean? = null,
        val subscriptionStartDate: Long? = null,
        val subscriptionEndDate: Long? = null,
        val subscriptionActivateDate: Long? = null,
        val autoRenewEnabled: Boolean? = null,
        val contentCountDescription: String? = null,
        val subscriptionBundle: Boolean? = null,
        val products: ProductListResponse? = null,
        val collectionItems: ItemListElementResponse? = null
)