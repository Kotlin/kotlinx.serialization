import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.json.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class Data(
        val id: Int,
        @Serializable(with = IX::class) val payload: Payload,
        @ContextualSerialization val date: Date
)

data class Payload(val content: String)

@Serializer(forClass = Payload::class)
object IX {}

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    private val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS")

    override fun serialize(output: Encoder, obj: Date) {
        output.encodeString(df.format(obj))
    }

    override fun deserialize(decoder: Decoder): Date {
        return df.parse(decoder.decodeString())
    }
}

fun main(args: Array<String>) {
    val o = Data(1, Payload("lorem ipsum dolor sit amet"), Date())
    val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true), context = serializersModule(DateSerializer))
    println(json.stringify(Data.serializer(), o))
}
