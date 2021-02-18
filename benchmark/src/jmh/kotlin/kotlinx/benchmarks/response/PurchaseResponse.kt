package response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

// TODO: remove?
@Serializable
@JsonClass(generateAdapter = true)
data class PurchaseResponse(
        val transactionInfo: TransactionInfoResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class TransactionInfoResponse(
        @Json(name = "id")
        val id: String? = ""
)