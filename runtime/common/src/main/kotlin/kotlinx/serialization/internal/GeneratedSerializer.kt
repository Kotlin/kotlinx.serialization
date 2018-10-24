package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor

interface GeneratedSerializer<T> : KSerializer<T> {
    fun childSerializers(): Array<KSerializer<*>>
}
