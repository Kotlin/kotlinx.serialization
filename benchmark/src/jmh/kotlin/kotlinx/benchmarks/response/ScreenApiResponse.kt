package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
@JsonClass(generateAdapter = true)
@Suppress("Detekt.LongParameterList")
data class ScreenApiResponse(
        val status: Int? = null,
        val authInfo: AuthInfoResponse? = null,
        val tournament: ElementResponse? = null,
        val myMovies: ElementResponse? = null,
        val serviceInfo: ServiceInfoResponse? = null,
        val userInfo: UserInfoResponse? = null,
        val paymentStatus: PaymentStatusResponse? = null,
        val paymentInfo: PaymentInfoResponse? = null,
        val element: ElementResponse? = null,
        val elements: ElementsListResponse? = null,
        val activeSubscriptions: CollectionElementResponse? = null,
        val upgradeSubscriptions: CollectionElementResponse? = null,
        val lastElement: ElementResponse? = null,
        val uiScreenInfo: UiScreenInfoResponse? = null,
        val availableOffers: OfferListResponse? = null,
        val transactionInfo: TransactionInfoResponse? = null
) : JsonValueResponse {

    @Transient
    @kotlin.jvm.Transient
    override var jsonValue: JsonElement? = null

    fun getClientAttribute(name: String) = uiScreenInfo?.attributes?.get(name)

    fun getServerTime() = serviceInfo?.serverTime ?: 0

}