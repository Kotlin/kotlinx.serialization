package kotlinx.benchmarks.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * All model classes are the same as in MacroTwitter.kt but named accordingly to Kotlin naming policies to test JsonNamingStrategy performance.
 * Only Size, SizeType and Urls are not copied
 */

@Serializable
data class MacroTwitterFeedKt(
    val statuses: List<TwitterStatusKt>,
    val searchMetadata: SearchMetadata
)

@Serializable
data class MicroTwitterFeedKt(
    val statuses: List<TwitterTrimmedStatusKt>
)

@Serializable
data class TwitterTrimmedStatusKt(
    val metadata: MetadataKt,
    val createdAt: String,
    val id: Long,
    val idStr: String,
    val text: String,
    val source: String,
    val truncated: Boolean,
    val user: TwitterTrimmedUserKt,
    val retweetedStatus: TwitterTrimmedStatusKt? = null,
)

@Serializable
data class TwitterStatusKt(
    val metadata: MetadataKt,
    val createdAt: String,
    val id: Long,
    val idStr: String,
    val text: String,
    val source: String,
    val truncated: Boolean,
    val inReplyToStatusId: Long?,
    val inReplyToStatusIdStr: String?,
    val inReplyToUserId: Long?,
    val inReplyToUserIdStr: String?,
    val inReplyToScreenName: String?,
    val user: TwitterUserKt,
    val geo: String?,
    val coordinates: String?,
    val place: String?,
    val contributors: List<String>?,
    val retweetedStatus: TwitterStatusKt? = null,
    val retweetCount: Int,
    val favoriteCount: Int,
    val entities: StatusEntitiesKt,
    val favorited: Boolean,
    val retweeted: Boolean,
    val lang: String,
    val possiblySensitive: Boolean? = null
)

@Serializable
data class StatusEntitiesKt(
    val hashtags: List<Hashtag>,
    val symbols: List<String>,
    val urls: List<Url>,
    val userMentions: List<TwitterUserMentionKt>,
    val media: List<TwitterMediaKt>? = null
)

@Serializable
data class TwitterMediaKt(
    val id: Long,
    val idStr: String,
    val url: String,
    val mediaUrl: String,
    val mediaUrlHttps: String,
    val expandedUrl: String,
    val displayUrl: String,
    val indices: List<Int>,
    val type: String,
    val sizes: SizeType,
    val sourceStatusId: Long? = null,
    val sourceStatusIdStr: String? = null
)

@Serializable
data class TwitterUserMentionKt(
    val screenName: String,
    val name: String,
    val id: Long,
    val idStr: String,
    val indices: List<Int>
)

@Serializable
data class MetadataKt(
    val resultType: String,
    val isoLanguageCode: String
)

@Serializable
data class TwitterTrimmedUserKt(
    val id: Long,
    val idStr: String,
    val name: String,
    val screenName: String,
    val location: String,
    val description: String,
    val url: String?,
    val entities: UserEntitiesKt,
    val protected: Boolean,
    val followersCount: Int,
    val friendsCount: Int,
    val listedCount: Int,
    val createdAt: String,
    val favouritesCount: Int,
)

@Serializable
data class TwitterUserKt(
    val id: Long,
    val idStr: String,
    val name: String,
    val screenName: String,
    val location: String,
    val description: String,
    val url: String?,
    val entities: UserEntitiesKt,
    val protected: Boolean,
    val followersCount: Int,
    val friendsCount: Int,
    val listedCount: Int,
    val createdAt: String,
    val favouritesCount: Int,
    val utcOffset: Int?,
    val timeZone: String?,
    val geoEnabled: Boolean,
    val verified: Boolean,
    val statusesCount: Int,
    val lang: String,
    val contributorsEnabled: Boolean,
    val isTranslator: Boolean,
    val isTranslationEnabled: Boolean,
    val profileBackgroundColor: String,
    val profileBackgroundImageUrl: String,
    val profileBackgroundImageUrlHttps: String,
    val profileBackgroundTile: Boolean,
    val profileImageUrl: String,
    val profileImageUrlHttps: String,
    val profileBannerUrl: String? = null,
    val profileLinkColor: String,
    val profileSidebarBorderColor: String,
    val profileSidebarFillColor: String,
    val profileTextColor: String,
    val profileUseBackgroundImage: Boolean,
    val defaultProfile: Boolean,
    val defaultProfileImage: Boolean,
    val following: Boolean,
    val followRequestSent: Boolean,
    val notifications: Boolean
)

@Serializable
data class UserEntitiesKt(
    val url: Urls? = null,
    val description: Urls
)
