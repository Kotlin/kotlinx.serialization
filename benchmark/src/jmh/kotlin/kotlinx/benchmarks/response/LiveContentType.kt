package response

import kotlinx.serialization.Serializable

@Serializable
enum class LiveContentType {

    GENERAL,
    LIVE_GAME,
    FULL_GAME,
    PREVIEW_SHORT,
    PREVIEW,
    REVIEW_SHORT,
    REVIEW,
    HIGHLIGHTS,
    STATISTICS,
    AUTHORS,

    LIVE_ON_AIR,
    LIVE_RECORD;

    val isLive: Boolean
        get() = this == LIVE_ON_AIR || this == LIVE_GAME

}