package response

enum class ActivationActionType {

    EMAIL_LEAVING,
    EMAIL_CONFIRMATION,
    PHONE_CONFIRMATION,
    REGISTER,
    REGISTER_BY_PHONE,
    LOGIN,
    SUBSCRIPTION_PURCHASE,
    SUBSCRIPTION_PAID,
    MOVIE_PURCHASE;

    fun isSubscriptionPurchaseAction() = this == SUBSCRIPTION_PURCHASE || this == SUBSCRIPTION_PAID

}