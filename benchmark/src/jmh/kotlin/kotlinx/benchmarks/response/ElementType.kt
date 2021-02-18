package response

import kotlinx.serialization.Serializable

@Serializable
enum class ElementType {
    MOVIE,
    MP_MOVIE,
    SERIAL,
    LIVE_EVENT,
    COLLECTION,
    GAME,
    PROGRAM,
    SPORT_COLLECTION,
    TOURNAMENT,
    TEAM,
    TOUR,
    UNKNOWN,
    SEASON,
    EPISODE,
    GENRE,
    PERSON,
    COUPON,
    SUBSCRIPTION,
    COUNTRY,
    AWARD,
    CHANNEL,
    SEARCH_RESULTS,
    PROMO,
    TV_CHANNEL,
    TV_EVENT
}