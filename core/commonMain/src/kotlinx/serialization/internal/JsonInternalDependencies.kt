package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

/*
 * Methods that are required for kotlinx-serialization-json, but are not effectively public
 * and actually represent our own technical debt.
 * This methods are not intended for public use
 */

@InternalSerializationApi
@Deprecated(message = "Should not be used", level = DeprecationLevel.ERROR)
public fun SerialDescriptor.jsonCachedSerialNames(): Set<String> = cachedSerialNames()
