package kotlinx.benchmarks.model

import kotlinx.serialization.Serializable


@Serializable
data class CitmCatalog(
    val areaNames: Map<String, String>,
    val blockNames: Map<String, String>,
    val events: Map<String, CitmEvent>,
    val audienceSubCategoryNames: Map<String, String>,
    val performances: List<CitmPerformance>,
    val seatCategoryNames: Map<String, String>,
    val subTopicNames: Map<String, String>,
    val subjectNames: Map<String, String>,
    val topicNames: Map<String, String>,
    val topicSubTopics: Map<String, List<Int>>,
    val venueNames: Map<String, String>
)

@Serializable
data class CitmPerformance(
    val eventId: Int,
    val id: Int,
    val logo: String?,
    val name: String?,
    val prices: List<CitmPrice>,
    val seatCategories: List<CitmSeatCategory>,
    val seatMapImage: String?,
    val start: Long,
    val venueCode: String
)

@Serializable
data class CitmSeatCategory(
    val areas: List<CitmArea>,
    val seatCategoryId: Int
)

@Serializable
data class CitmArea(val areaId: Int, val blockIds: List<String>)

@Serializable
data class CitmPrice(
    val amount: Int,
    val audienceSubCategoryId: Int,
    val seatCategoryId: Int
)

@Serializable
data class CitmEvent(
    val description: String?,
    val id: Int?,
    val logo: String?,
    val name: String,
    val subTopicIds: List<Int>,
    val subjectCode: Int?,
    val subtitle: String?,
    val topicIds: List<Int>
)
