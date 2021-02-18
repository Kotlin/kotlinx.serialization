package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class CollectionListHolderResponse(
        val id: String,
        val collectionItems: CollectionListResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class CollectionListResponse(
        val items: List<CollectionResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class CollectionResponse(
        val element: ElementResponse,
        val userInfo: UserInfoResponse? = null,
        val serviceInfo: ServiceInfoHolderResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class CollectionElementResponse(
        val id: String,
        val name: String? = null,
        val alias: String? = null,
        val type: String? = null,
        val collectionItems: ItemListElementResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class ItemListElementResponse(
        val items: List<ItemElementHolderResponse>? = null,
        val totalSize: Int?
)