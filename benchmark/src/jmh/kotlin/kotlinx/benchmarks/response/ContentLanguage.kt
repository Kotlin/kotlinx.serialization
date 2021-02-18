package response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContentLanguage {

    @SerialName("rus")
    @Json(name = "rus")
    RUS,
    @SerialName("eng")
    @Json(name = "eng")
    ENG,
    @SerialName("fre")
    @Json(name = "fre")
    FRE,
    @SerialName("ger")
    @Json(name = "ger")
    GER,
    @SerialName("ita")
    @Json(name = "ita")
    ITA,
    @SerialName("spa")
    @Json(name = "spa")
    SPA,
    @SerialName("int")
    @Json(name = "int")
    INT,
    @SerialName("none")
    @Json(name = "none")
    NONE;

}