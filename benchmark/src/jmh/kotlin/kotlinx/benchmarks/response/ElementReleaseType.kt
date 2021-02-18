package response

import kotlinx.serialization.Serializable

@Serializable
enum class ElementReleaseType {
    RELEASE,
    ANNOUNCE
}