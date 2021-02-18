package response

import kotlinx.serialization.Serializable

@Serializable
enum class UiType {

    FEATURED,
    RAIL,
    GRID,
    BANNER

}