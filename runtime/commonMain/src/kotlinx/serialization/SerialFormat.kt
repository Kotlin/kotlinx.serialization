/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

interface SerialFormat {
    val context: SerialModule
}

@Deprecated(
    "Deprecated for removal since it is indistinguishable from SerialFormat interface. " +
            "Use SerialFormat instead.", ReplaceWith("SerialFormat"), DeprecationLevel.WARNING
)
abstract class AbstractSerialFormat(override val context: SerialModule) : SerialFormat

interface BinaryFormat : SerialFormat {
    fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray
    fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, obj: T): String =
    InternalHexConverter.printHexBinary(dump(serializer, obj), lowerCase = true)

fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, InternalHexConverter.parseHexBinary(hex))

interface StringFormat : SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
