package kotlinx.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass
import kotlin.test.Test

// TODO: for this test to work, kotlin dependency should be updated
// to serialization plugin with @MetaSerializable support
class MetaSerializableTest {

    @MetaSerializable
    @Target(AnnotationTarget.CLASS)
    annotation class MySerializableWithCustomSerializer(@MetaSerializable.Serializer val with: KClass<out KSerializer<*>> = KSerializer::class)

    @MetaSerializable
    @Target(AnnotationTarget.CLASS)
    annotation class MySerializableWithInfo(val value: Int, val klass: KClass<*>)

    @MySerializableWithCustomSerializer(MySerializer::class)
    class Project1(val name: String, val language: String)

    @MySerializableWithCustomSerializer
    class Project2(val name: String, val language: String)

    @MySerializableWithInfo(value = 123, String::class)
    class Project3(val name: String, val language: String)

    object MySerializer : KSerializer<Project1> {
        override val descriptor: SerialDescriptor
            get() = throw NotImplementedError()

        override fun serialize(encoder: Encoder, value: Project1) = throw NotImplementedError()
        override fun deserialize(decoder: Decoder): Project1 = throw NotImplementedError()
    }

    @Test
    fun testCustomSerializer() {
//        val serializer = serializer<Project1>()
//        assertEquals(serializer, MySerializer)
    }

    @Test
    fun testDefaultSerializer() {
//        val serializer = serializer<Project2>()
//        assertNotNull(serializer)
    }

    @Test
    fun testDefaultSerialInfo() {
//        val descriptor = serializer<Project3>().descriptor
//        val annotation = descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
//        assertEquals(123, annotation.value)
//        assertEquals(String::class, annotation.klass)
    }
}
