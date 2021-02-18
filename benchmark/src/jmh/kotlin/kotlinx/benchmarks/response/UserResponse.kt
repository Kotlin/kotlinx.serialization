package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class UserInfoResponse(
        val id: String = "",
        val offers: OfferListResponse? = null,
        val preferences: UserPreferencesResponse? = null,
        val billingAccounts: BillingAccountListResponse? = null,
        val paymentMethods: PaymentMethodListResponse? = null,
        val multiSubscriptions: ElementRelationListResponse? = null,
        val devices: DeviceItemsResponse? = null,
        val logoutAvailable: Boolean? = null,
        val sportEplActivated: Boolean? = null,
        val phone: String? = null,
        val phoneConfirmed: Boolean? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class AuthInfoResponse(
        val sessionToken: String,
        val accessKey: String,
        val persistentToken: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class NotificationItemsHolder(
        val items: List<NotificationResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class NotificationResponse(
        val id: String? = null,
        val type: String? = null,
        val layoutUrl: String? = null,
        val layoutUrl1280: String? = null,
        val actions: List<ActionResponse>? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class LocationResponse(
        val countryCode: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class ActionResponse(
        val type: String,
        val name: String,
        val element: ElementResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class PaymentMethodListResponse(
        val items: List<PaymentMethodResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class PaymentMethodResponse(
        val type: String,
        val creditCard: CreditCardResponse? = null,
        val qiwiWallet: QiwiWalletResponse? = null,
        val description: String? = null,
        val bonusBalance: Double? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class CreditCardResponse(
        val id: String,
        val type: String? = null,
        val number: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class QiwiWalletResponse(
        val id: String,
        val phone: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class BillingAccountListResponse(
        val items: List<BillingAccountResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class BillingAccountResponse(
        val id: String,
        val balance: BalanceResponse,
        val reserve: BalanceResponse? = null,
        val description: String? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class BalanceResponse(
        val value: Double,
        val currencyCode: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class UserPreferencesResponse(
        @SerialName("epl.spoilers.allowed")
        val isShowScoreAllowed: String? = null,
        @SerialName("epl.sms.requested")
        val isEplNotifyRequested: String? = null,
        @SerialName("yv.payment.topup.phone.last")
        val lastPhone: String? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class DeviceItemsResponse(
        val items: List<DeviceItemResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class DeviceItemResponse(
        val id: String,
        val manufacturer: String? = null,
        val model: String? = null
)