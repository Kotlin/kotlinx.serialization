/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.internal.InternalHexConverter

private const val INSTALL_DEPRECATION_TEXT = "Install is no longer supported, module can be added to format only in constructor"

interface SerialFormat {
    val context: SerialModule

    @Deprecated(INSTALL_DEPRECATION_TEXT, level = DeprecationLevel.ERROR)
    fun install(module: SerialModule) {}
}

abstract class AbstractSerialFormat(override val context: SerialModule): SerialFormat {
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(INSTALL_DEPRECATION_TEXT, level = DeprecationLevel.ERROR)
    final override fun install(module: SerialModule) {
        throw NotImplementedError(INSTALL_DEPRECATION_TEXT)
    }
}

interface BinaryFormat: SerialFormat {
    fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray
    fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, obj: T): String =
    InternalHexConverter.printHexBinary(dump(serializer, obj), lowerCase = true)

fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, InternalHexConverter.parseHexBinary(hex))

interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
