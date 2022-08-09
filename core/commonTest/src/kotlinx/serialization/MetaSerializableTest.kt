package kotlinx.serialization

import kotlinx.serialization.test.*
import kotlin.reflect.KClass
import kotlin.test.*

class MetaSerializableTest {

    @MetaSerializable
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    annotation class MySerializable

    @MetaSerializable
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    annotation class MySerializableWithInfo(
        val value: Int,
        val kclass: KClass<*>
    )

    @MySerializable
    class Project1(val name: String, val language: String)

    @MySerializableWithInfo(123, String::class)
    class Project2(val name: String, val language: String)

    @MySerializableWithInfo(123, String::class)
    @Serializable
    class Project3(val name: String, val language: String)

    @Serializable
    class Wrapper(
        @MySerializableWithInfo(234, Int::class) val project: Project3
    )

    @Test
    fun testMetaSerializable() = noJsLegacy {
        val serializer = serializer<Project1>()
        assertNotNull(serializer)
    }

    @Test
    fun testMetaSerializableWithInfo() = noJsLegacy {
        val info = serializer<Project2>().descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
        assertEquals(123, info.value)
        assertEquals(String::class, info.kclass)
    }

    @Test
    fun testMetaSerializableOnProperty() = noJsLegacy {
        val info = serializer<Wrapper>().descriptor
            .getElementAnnotations(0).filterIsInstance<MySerializableWithInfo>().first()
        assertEquals(234, info.value)
        assertEquals(Int::class, info.kclass)
    }
}
