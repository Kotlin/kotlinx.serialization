package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PaymentStatusResponse(
        val transactionStatus: String? = null,
        val errorReasonCode: Int? = null,
        val linkedCardLoyalties: List<String>? = emptyList()
)