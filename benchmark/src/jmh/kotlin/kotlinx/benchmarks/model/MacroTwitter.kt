package kotlinx.benchmarks.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class MacroTwitterFeed(
    val statuses: List<TwitterStatus>,
    val search_metadata: SearchMetadata
)

@Serializable
data class MicroTwitterFeed(
    val statuses: List<TwitterTrimmedStatus>
)

@Serializable
data class TwitterTrimmedStatus(
    val metadata: Metadata,
    val created_at: String,
    val id: Long,
    val id_str: String,
    val text: String,
    val source: String,
    val truncated: Boolean,
    val user: TwitterTrimmedUser,
    val retweeted_status: TwitterTrimmedStatus? = null,
)

@Serializable
data class TwitterStatus(
    val metadata: Metadata,
    val created_at: String,
    val id: Long,
    val id_str: String,
    val text: String,
    val source: String,
    val truncated: Boolean,
    val in_reply_to_status_id: Long?,
    val in_reply_to_status_id_str: String?,
    val in_reply_to_user_id: Long?,
    val in_reply_to_user_id_str: String?,
    val in_reply_to_screen_name: String?,
    val user: TwitterUser,
    val geo: String?,
    val coordinates: String?,
    val place: String?,
    val contributors: List<String>?,
    val retweeted_status: TwitterStatus? = null,
    val retweet_count: Int,
    val favorite_count: Int,
    val entities: StatusEntities,
    val favorited: Boolean,
    val retweeted: Boolean,
    val lang: String,
    val possibly_sensitive: Boolean? = null
)

@Serializable
data class StatusEntities(
    val hashtags: List<Hashtag>,
    val symbols: List<String>,
    val urls: List<Url>,
    val user_mentions: List<TwitterUserMention>,
    val media: List<TwitterMedia>? = null
)

@Serializable
data class TwitterMedia(
    val id: Long,
    val id_str: String,
    val url: String,
    val media_url: String,
    val media_url_https: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>,
    val type: String,
    val sizes: SizeType,
    val source_status_id: Long? = null,
    val source_status_id_str: String? = null
)

@Serializable
data class SizeType(
    val large: Size,
    val medium: Size,
    val thumb: Size,
    val small: Size
)

@Serializable
data class Size(val w: Int, val h: Int, val resize: String)

@Serializable
data class TwitterUserMention(
    val screen_name: String,
    val name: String,
    val id: Long,
    val id_str: String,
    val indices: List<Int>
)

@Serializable
data class Urls(val urls: List<Url>)

@Serializable
data class Metadata(
    val result_type: String,
    val iso_language_code: String
)

@Serializable
data class TwitterTrimmedUser(
    val id: Long,
    val id_str: String,
    val name: String,
    val screen_name: String,
    val location: String,
    val description: String,
    val url: String?,
    val entities: UserEntities,
    val protected: Boolean,
    val followers_count: Int,
    val friends_count: Int,
    val listed_count: Int,
    val created_at: String,
    val favourites_count: Int,
)

@Serializable
data class TwitterUser(
    val id: Long,
    val id_str: String,
    val name: String,
    val screen_name: String,
    val location: String,
    val description: String,
    val url: String?,
    val entities: UserEntities,
    val protected: Boolean,
    val followers_count: Int,
    val friends_count: Int,
    val listed_count: Int,
    val created_at: String,
    val favourites_count: Int,
    val utc_offset: Int?,
    val time_zone: String?,
    val geo_enabled: Boolean,
    val verified: Boolean,
    val statuses_count: Int,
    val lang: String,
    val contributors_enabled: Boolean,
    val is_translator: Boolean,
    val is_translation_enabled: Boolean,
    val profile_background_color: String,
    val profile_background_image_url: String,
    val profile_background_image_url_https: String,
    val profile_background_tile: Boolean,
    val profile_image_url: String,
    val profile_image_url_https: String,
    val profile_banner_url: String? = null,
    val profile_link_color: String,
    val profile_sidebar_border_color: String,
    val profile_sidebar_fill_color: String,
    val profile_text_color: String,
    val profile_use_background_image: Boolean,
    val default_profile: Boolean,
    val default_profile_image: Boolean,
    val following: Boolean,
    val follow_request_sent: Boolean,
    val notifications: Boolean
)

@Serializable
data class UserEntities(
    val url: Urls? = null,
    val description: Urls
)

fun main() {
    val s = MacroTwitterFeed::class.java.getResource("/twitter_macro.json").readBytes().decodeToString()
    println(Json.decodeFromString<MacroTwitterFeed>(s))
}
