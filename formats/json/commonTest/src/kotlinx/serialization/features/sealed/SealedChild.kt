package kotlinx.serialization.features.sealed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("first child")
data class SealedChild(val j: Int) : SealedParent(1)
