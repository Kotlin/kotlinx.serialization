package kotlinx.serialization.hocon.internal

import kotlin.time.Duration
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Returns `true` if this descriptor is equals to descriptor in [kotlinx.serialization.internal.DurationSerializer].
 */
internal val SerialDescriptor.isDuration: Boolean
    get() = this == Duration.serializer().descriptor
