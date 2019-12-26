/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule

import org.junit.Test
import java.text.*
import java.util.*
import kotlin.test.assertEquals

class PolymorphicWithJvmClassTest {
    @Serializable
    data class DateWrapper(@SerialId(1) @Serializable(with = PolymorphicSerializer::class) val date: Date)

    @Serializer(forClass = Date::class)
    object DateSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("java.util.Date", PrimitiveKind.STRING)

        // Consider wrapping in ThreadLocal if serialization may happen in multiple threads
        private val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").apply {
            timeZone = TimeZone.getTimeZone("GMT+2")
        }

        override fun serialize(encoder: Encoder, obj: Date) {
            encoder.encodeString(df.format(obj))
        }

        override fun deserialize(decoder: Decoder): Date {
            return df.parse(decoder.decodeString())
        }
    }

    @Test
    fun testPolymorphicWrappedOverride() {
        val protobuf = ProtoBuf(context = SerializersModule { polymorphic(Date::class, DateSerializer) })
        val obj = DateWrapper(Date())
        val bytes = protobuf.dumps(obj)
        val restored = protobuf.loads<DateWrapper>(bytes)
        assertEquals(obj, restored)
    }
}
