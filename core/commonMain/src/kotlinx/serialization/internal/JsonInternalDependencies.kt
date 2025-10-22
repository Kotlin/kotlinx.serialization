package kotlinx.serialization.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.*

/*
 * Methods that are required for kotlinx-serialization-json, but are not effectively public.
 *
 * Anything marked with this annotation is not intended for public use.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
internal annotation class CoreFriendModuleApi

@CoreFriendModuleApi
public fun SerialDescriptor.jsonCachedSerialNames(): Set<String> = cachedSerialNames()

@ExperimentalSerializationApi
@CoreFriendModuleApi
public fun missingFieldExceptionWithNewMessage(exception: MissingFieldException, message: String): MissingFieldException =
    exception.withNewMessageInternal(message)