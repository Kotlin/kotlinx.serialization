/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

/**
 * Encoder that does not do any operations. Its main purpose is to ignore data instead of writing it.
 */
@ExperimentalSerializationApi
internal object EmptyEncoder : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    public override fun encodeValue(value: Any): Unit = Unit

    override fun encodeNull(): Unit = Unit

    override fun encodeBoolean(value: Boolean): Unit = Unit
    override fun encodeByte(value: Byte): Unit = Unit
    override fun encodeShort(value: Short): Unit = Unit
    override fun encodeInt(value: Int): Unit = Unit
    override fun encodeLong(value: Long): Unit = Unit
    override fun encodeFloat(value: Float): Unit = Unit
    override fun encodeDouble(value: Double): Unit = Unit
    override fun encodeChar(value: Char): Unit = Unit
    override fun encodeString(value: String): Unit = Unit
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = Unit
}
