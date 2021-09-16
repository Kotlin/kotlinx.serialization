import kotlinx.serialization.KSerializer
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass
import kotlin.test.Test

// TODO: for this test to work, kotlin dependency should be updated
// to serialization plugin with @MetaSerializable support

@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializable(
    @MetaSerializable.Serializer val with: KClass<out KSerializer<*>> = KSerializer::class,
)

@MySerializable(MySerializer::class)
class Project1(val name: String, val language: String)

@MySerializable
class Project2(val name: String, val language: String)

object MySerializer : KSerializer<Project1> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Project", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Project1) = encoder.encodeString("${value.name}:${value.language}")

    override fun deserialize(decoder: Decoder): Project1 {
        val params = decoder.decodeString().split(':')
        return Project1(params[0], params[1])
    }
}

class Test {

    @Test
    fun testCustomSerializer() {
//        val string = Json.encodeToString(serializer(), Project1("name", "lang"))
//        assertEquals("\"name:lang\"", string)
//        val reconstructed = Json.decodeFromString(serializer<Project1>(), string)
//        assertEquals("name", reconstructed.name)
//        assertEquals("lang", reconstructed.language)
    }

    @Test
    fun testDefaultSerializer() {
//        val string = Json.encodeToString(serializer(), Project2("name", "lang"))
//        assertEquals("""{"name":"name","language":"lang"}""", string)
//        val reconstructed = Json.decodeFromString(serializer<Project2>(), string)
//        assertEquals("name", reconstructed.name)
//        assertEquals("lang", reconstructed.language)
    }
}
