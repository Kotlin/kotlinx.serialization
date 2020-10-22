package kotlinx.serialization.hocon

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.*
import org.junit.Assert.*
import org.junit.Test

class HoconPolymorphismTest {
    @Serializable
    sealed class Sealed(val intField: Int) {
        @Serializable
        @SerialName("object")
        object ObjectChild : Sealed(0)

        @Serializable
        @SerialName("data_class")
        data class DataClassChild(val name: String) : Sealed(1)

        @Serializable
        @SerialName("type_child")
        data class TypeChild(val type: String) : Sealed(2)

        @Serializable
        @SerialName("annotated_type_child")
        data class AnnotatedTypeChild(@SerialName("my_type") val type: String) : Sealed(3)
    }

    @Serializable
    data class CompositeClass(var sealed: Sealed)


    private val arrayHocon = Hocon {
        useArrayPolymorphism = true
    }

    private val objectHocon = Hocon {
        useArrayPolymorphism = false
    }


    @Test
    fun testArrayDataClass() {
        val config = ConfigFactory.parseString(
                """{
                sealed: [
                  "data_class"
                  {name="testArrayDataClass"
                   intField=10}
                ]
                }""")
        val root = arrayHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("testArrayDataClass", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testArrayObject() {
        val config = ConfigFactory.parseString(
                """{
                sealed: [
                  "object"
                  {}
                ]
                }""")
        val root = arrayHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertSame(Sealed.ObjectChild, sealed)
    }

    @Test
    fun testObject() {
        val config = ConfigFactory.parseString("""{type="object"}""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertSame(Sealed.ObjectChild, sealed)
    }

    @Test
    fun testNestedDataClass() {
        val config = ConfigFactory.parseString(
                """{
                sealed: {
                  type="data_class"
                  name="test name"
                  intField=10
                }
                }""")
        val root = objectHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("test name", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testDataClass() {
        val config = ConfigFactory.parseString(
                """{
                  type="data_class"
                  name="testDataClass"
                  intField=10
                }""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("testDataClass", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testChangeDiscriminator() {
        val hocon = Hocon(objectHocon) {
            classDiscriminator = "key"
        }

        val config = ConfigFactory.parseString(
                """{
                  type="override"
                  key="type_child"
                  intField=11
                }""")
        val sealed = hocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.TypeChild)
        sealed as Sealed.TypeChild
        assertEquals("override", sealed.type)
        assertEquals(11, sealed.intField)
    }

    @Test
    fun testChangeTypePropertyName() {
        val config = ConfigFactory.parseString(
                """{
                  my_type="override"
                  type="annotated_type_child"
                  intField=12
                }""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.AnnotatedTypeChild)
        sealed as Sealed.AnnotatedTypeChild
        assertEquals("override", sealed.type)
        assertEquals(12, sealed.intField)
    }
}
