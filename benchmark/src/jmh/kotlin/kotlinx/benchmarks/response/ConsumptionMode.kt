package response

/**
 * Order is important. DTO = forever.
 */
enum class ConsumptionMode {

    DTO,
    RENT,
    SUBSCRIPTION;

    fun isTvod(): Boolean = this == RENT || this == DTO

    fun isSvod(): Boolean = this == SUBSCRIPTION

}