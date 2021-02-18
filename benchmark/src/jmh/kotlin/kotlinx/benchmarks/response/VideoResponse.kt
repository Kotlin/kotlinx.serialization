package response

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class ItemElementHolderResponse(
        val element: ElementResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class AssetListResponse(
        val items: List<AssetResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class AssetResponse(
        val liveContentType: LiveContentType? = null,
        val url: String,
        val licenseId: String? = null,
        val media: MediaResponse,
        val audioTracks: AudioTrackListResponse? = null,
        val subtitles: SubtitleListResponse? = null,
        // FIXME: use coerce input values https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#coercing-input-values
        // FIXME: and remove optional
        val failoverUrls: List<String>? = emptyList(),
        val wmData: String? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class MediaResponse(
        val quality: String,
        val mimeType: String,
        val drmType: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val size: Int? = null,
        val fps: Float? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class AudioTrackListResponse(
        val items: List<AudioTrackResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class AudioTrackResponse(
        val contentLanguage: ContentLanguage,
        val langKey: String? = null,
        val name: String? = null,
        val channels: AudioTrackChannel,
        val mimeType: MimeType
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubtitleListResponse(
        val items: List<SubtitleResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class SubtitleResponse(
        val url: String,
        val name: String,
        val contentLanguage: String,
        val forced: Boolean,
        val mimeType: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class GenreListResponse(
        val items: List<GenreHolderResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class GenreHolderResponse(
        val element: GenreResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class GenreResponse(
        val id: String,
        val name: String,
        val alias: String? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class CountryListResponse(
        val items: List<CountryHolderResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class CountryHolderResponse(
        val element: CountryResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class CountryResponse(
        val id: String,
        val name: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class PersonListResponse(
        val items: List<PersonHolderResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class PersonHolderResponse(
        val element: PersonResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class PersonResponse(
        val id: String,
        val alias: String?,
        val name: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class StudioListResponse(
        val items: List<StudioHolderResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class StudioHolderResponse(
        val element: StudioResponse
)

@Serializable
@JsonClass(generateAdapter = true)
data class StudioResponse(
        val id: String,
        val name: String,
        val covers: CoverElementHolderResponse? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class TrailerListResponse(
        val items: List<TrailerResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class TrailerResponse(
        val liveContentType: LiveContentType? = null,
        val url: String,
        val media: MediaResponse
) {

    val quality: String
        get() = media.quality
    val mimeType: String
        get() = media.mimeType

}

@Serializable
@JsonClass(generateAdapter = true)
data class LicenseListResponse(
        val items: List<LicenseResponse>
)

@Serializable
@JsonClass(generateAdapter = true)
data class LicenseResponse(
        val id: String? = null,
        val consumptionMode: ConsumptionMode? = null,
        val expireDate: Long? = null,
        val subscriptionId: String? = null,
        val licenseServerUrls: Map<String, String>? = emptyMap()
)