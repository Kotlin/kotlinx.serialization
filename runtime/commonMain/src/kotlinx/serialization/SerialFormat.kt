/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

public interface SerialFormat {
    public val context: SerialModule
}

@Deprecated(
    "Deprecated for removal since it is indistinguishable from SerialFormat interface. " +
            "Use SerialFormat instead.", ReplaceWith("SerialFormat"), DeprecationLevel.ERROR
)
public abstract class AbstractSerialFormat(override val context: SerialModule) : SerialFormat

public interface BinaryFormat : SerialFormat {
    public fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray
    public fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

public fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, value: T): String =
    InternalHexConverter.printHexBinary(dump(serializer, value), lowerCase = true)

public fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, InternalHexConverter.parseHexBinary(hex))

public interface StringFormat : SerialFormat {
    public fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String
    public fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
