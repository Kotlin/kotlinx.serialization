// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer16

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

import java.util.Date
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

@Serializable          
class ProgrammingLanguage(
    val name: String,
    val releaseDates: List<@Serializable(DateAsLongSerializer::class) Date>
)

fun main() {
    val df = SimpleDateFormat("yyyy-MM-ddX")
    val data = ProgrammingLanguage("Kotlin", listOf(df.parse("2023-07-06+00"), df.parse("2023-04-25+00"), df.parse("2022-12-28+00")))
    println(Json.encodeToString(data))
}
