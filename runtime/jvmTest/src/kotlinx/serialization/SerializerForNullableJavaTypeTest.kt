/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import org.junit.Test
import java.util.*
import kotlin.test.*

class SerializerForNullableJavaTypeTest {

    // User-reported generic serialization
    class DateSerializer : KSerializer<Date?> {

        override val descriptor = PrimitiveSerialDescriptor("LocalTime?", PrimitiveKind.LONG).nullable

        override fun deserialize(decoder: Decoder): Date? = when (val seconds = decoder.decodeLong()) {
            -1L -> null
            else -> Date(seconds.toLong())
        }

        override fun serialize(encoder: Encoder, value: Date?) {
            when (value) {
                null -> encoder.encodeLong(-1L) //this line is never reached despite that nulls exist in serialized lists
                else -> encoder.encodeLong(value.toInstant().toEpochMilli())
            }
        }
    }

    @Serializable
    private data class ListWrapper(val times: List<@Serializable(with = DateSerializer::class) Date?>)

    @Test
    fun testMixedList() {
        val data = ListWrapper(listOf(Date(42), null))
        val str = Json.encodeToString(data)
        assertEquals("""{"times":[42,-1]}""", str)
        assertEquals(data, Json.decodeFromString(str))
    }
}
