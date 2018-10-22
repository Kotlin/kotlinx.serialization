package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JSON
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = StringDescriptor

    // Consider wrapping in ThreadLocal if serialization may happen in multiple threads
    private val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").apply {
        timeZone = TimeZone.getTimeZone("GMT+2")
    }

    override fun serialize(output: Encoder, obj: Date) {
        output.encodeString(df.format(obj))
    }

    override fun deserialize(input: Decoder): Date {
        return df.parse(input.decodeString())
    }
}

@Serializable
data class ClassWithDate(@Serializable(with = DateSerializer::class) val date: Date)

class SerializeJavaClassTest {
    @Test
    fun serializeToStringAndRestore() {
        // Thursday, 4 October 2018 09:00:00 GMT+02:00 — KotlinConf 2018 Keynote
        val date = ClassWithDate(Date(1538636400000L))
        val s = JSON.stringify(date)
        assertEquals("""{"date":"04/10/2018 09:00:00.000"}""", s)
        val date2 = JSON.parse(ClassWithDate.serializer(), s)
        assertEquals(date, date2)
    }
}
