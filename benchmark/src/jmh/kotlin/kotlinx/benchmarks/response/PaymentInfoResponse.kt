package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PaymentInfoResponse(
        val url: String? = null,
        val token: String? = null,
        val qiwiWalletAuthRequestCode: String? = null
)