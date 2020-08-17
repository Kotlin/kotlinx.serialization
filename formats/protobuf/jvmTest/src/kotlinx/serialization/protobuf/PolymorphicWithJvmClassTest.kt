/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import org.junit.Test
import java.text.*
import java.util.*
import kotlin.test.*

class PolymorphicWithJvmClassTest {
    @Serializable
    data class DateWrapper(@ProtoNumber(1) @Serializable(with = PolymorphicSerializer::class) val date: Date)

    @Serializer(forClass = Date::class)
    object DateSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.util.Date", PrimitiveKind.STRING)

        // Consider wrapping in ThreadLocal if serialization may happen in multiple threads
        private val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").apply {
            timeZone = TimeZone.getTimeZone("GMT+2")
        }

        override fun serialize(encoder: Encoder, value: Date) {
            encoder.encodeString(df.format(value))
        }

        override fun deserialize(decoder: Decoder): Date {
            return df.parse(decoder.decodeString())
        }
    }

    @Test
    fun testPolymorphicWrappedOverride() {
        val protobuf = ProtoBuf { serializersModule = SerializersModule { polymorphic(Date::class, DateSerializer) } }
        val obj = DateWrapper(Date())
        val bytes = protobuf.encodeToHexString(obj)
        val restored = protobuf.decodeFromHexString<DateWrapper>(bytes)
        assertEquals(obj, restored)
    }
}
