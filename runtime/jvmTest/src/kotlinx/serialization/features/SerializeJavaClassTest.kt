/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals

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

@Serializable
data class ClassWithDate(@Serializable(with = DateSerializer::class) val date: Date)

class SerializeJavaClassTest {
    @Test
    fun serializeToStringAndRestore() {
        // Thursday, 4 October 2018 09:00:00 GMT+02:00 — KotlinConf 2018 Keynote
        val date = ClassWithDate(Date(1538636400000L))
        val s = Json.encodeToString(date)
        assertEquals("""{"date":"04/10/2018 09:00:00.000"}""", s)
        val date2 = Json.decodeFromString(ClassWithDate.serializer(), s)
        assertEquals(date, date2)
    }
}
