package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

/*
 * Methods that are required for kotlinx-serialization-json, but are not effectively public.
 *
 * Anything marker with this annotation is not intended for public use.
 *
 * This annotation is internal, so it is possible to opt-in only with module-wide -Xopt-in
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
internal annotation class SuperInternalSerializationApi

@SuperInternalSerializationApi
public fun SerialDescriptor.jsonCachedSerialNames(): Set<String> = cachedSerialNames()
