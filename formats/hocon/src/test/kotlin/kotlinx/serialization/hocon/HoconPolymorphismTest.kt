package kotlinx.serialization.hocon

import kotlinx.serialization.*
import org.junit.*

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
    data class SealedCollectionContainer(val sealed: Collection<Sealed>)

    @Serializable
    data class SealedMapContainer(val sealed: Map<String, Sealed>)

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
        arrayHocon.assertStringFormAndRestored(
            expected = "sealed: [ data_class, { name = testDataClass, intField = 1 } ]",
            original = CompositeClass(Sealed.DataClassChild("testDataClass")),
            serializer = CompositeClass.serializer(),
        )
    }

    @Test
    fun testArrayObject() {
        arrayHocon.assertStringFormAndRestored(
            expected = "sealed: [ object, {} ]",
            original = CompositeClass(Sealed.ObjectChild),
            serializer = CompositeClass.serializer(),
        )
    }

    @Test
    fun testObject() {
        objectHocon.assertStringFormAndRestored(
            expected = "type = object",
            original = Sealed.ObjectChild,
            serializer = Sealed.serializer(),
        )
    }

    @Test
    fun testNestedDataClass() {
        objectHocon.assertStringFormAndRestored(
            expected = "sealed { type = data_class, name = testDataClass, intField = 1 }",
            original = CompositeClass(Sealed.DataClassChild("testDataClass")),
            serializer = CompositeClass.serializer(),
        )
    }

    @Test
    fun testDataClassDecode() {
        objectHocon.assertStringFormAndRestored(
            expected = "type = data_class, name = testDataClass, intField = 1",
            original = Sealed.DataClassChild("testDataClass"),
            serializer = Sealed.serializer(),
        )
    }

    @Test
    fun testChangedDiscriminator() {
        val hocon = Hocon(objectHocon) {
            classDiscriminator = "key"
        }

        hocon.assertStringFormAndRestored(
            expected = "type = override, key = type_child, intField = 2",
            original = Sealed.TypeChild(type = "override"),
            serializer = Sealed.serializer(),
        )
    }

    @Test
    fun testChangedTypePropertyName() {
        objectHocon.assertStringFormAndRestored(
            expected = "type = annotated_type_child, my_type = override, intField = 3",
            original = Sealed.AnnotatedTypeChild(type = "override"),
            serializer = Sealed.serializer(),
        )
    }

    @Test
    fun testCollectionContainer() {
        objectHocon.assertStringFormAndRestored(
            expected = """
                sealed = [ 
                    { type = annotated_type_child, my_type = override, intField = 3 }
                    { type = object }
                    { type = data_class, name = testDataClass, intField = 1 }
                ]
            """.trimIndent(),
            original = SealedCollectionContainer(
                listOf(
                    Sealed.AnnotatedTypeChild(type = "override"),
                    Sealed.ObjectChild,
                    Sealed.DataClassChild(name = "testDataClass"),
                )
            ),
            serializer = SealedCollectionContainer.serializer(),
        )
    }

    @Test
    fun testMapContainer() {
        objectHocon.assertStringFormAndRestored(
            expected = """
                sealed = { 
                    "annotated_type_child" = { type = annotated_type_child, my_type = override, intField = 3 }
                    "object" = { type = object }
                    "data_class" = { type = data_class, name = testDataClass, intField = 1 }
                }
            """.trimIndent(),
            original = SealedMapContainer(
                mapOf(
                    "annotated_type_child" to Sealed.AnnotatedTypeChild(type = "override"),
                    "object" to Sealed.ObjectChild,
                    "data_class" to Sealed.DataClassChild(name = "testDataClass"),
                )
            ),
            serializer = SealedMapContainer.serializer(),
        )
    }
}
