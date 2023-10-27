package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class JsonElementDecodingTest : JsonTestBase() {

    @Serializable
    data class A(val a: Int = 42)

    @Test
    fun testTopLevelClass() = assertSerializedForm(A(), """{}""".trimMargin())

    @Test
    fun testTopLevelNullableClass() {
        assertSerializedForm<A?>(A(), """{}""")
        assertSerializedForm<A?>(null, "null")
    }

    @Test
    fun testTopLevelPrimitive() = assertSerializedForm(42, """42""")

    @Test
    fun testTopLevelNullablePrimitive() {
        assertSerializedForm<Int?>(42, """42""")
        assertSerializedForm<Int?>(null, """null""")
    }

    @Test
    fun testTopLevelList() = assertSerializedForm(listOf(42), """[42]""")

    @Test
    fun testTopLevelNullableList() {
        assertSerializedForm<List<Int>?>(listOf(42), """[42]""")
        assertSerializedForm<List<Int>?>(null, """null""")
    }

    private inline fun <reified T> assertSerializedForm(value: T, expectedString: String) {
        val element = Json.encodeToJsonElement(value)
        assertEquals(expectedString, element.toString())
        assertEquals(value, Json.decodeFromJsonElement(element))
    }

    @Test
    fun testDeepRecursion() {
        // Reported as https://github.com/Kotlin/kotlinx.serialization/issues/1594
        var json = """{ "a": %}"""
        for (i in 0..12) {
            json = json.replace("%", json)
        }
        json = json.replace("%", "0")
        Json.parseToJsonElement(json)
    }

    private open class NullAsElementSerializer<T : Any>(private val serializer: KSerializer<T>, val nullElement: T) : KSerializer<T?> {
        final override val descriptor: SerialDescriptor = serializer.descriptor.nullable

        final override fun serialize(encoder: Encoder, value: T?) {
            serializer.serialize(encoder, value ?: nullElement)
        }

        final override fun deserialize(decoder: Decoder): T = serializer.deserialize(decoder)
    }

    private object NullAsJsonNullJsonElementSerializer : NullAsElementSerializer<JsonElement>(JsonElement.serializer(), JsonNull)
    private object NullAsJsonNullJsonPrimitiveSerializer : NullAsElementSerializer<JsonPrimitive>(JsonPrimitive.serializer(), JsonNull)
    private object NullAsJsonNullJsonNullSerializer : NullAsElementSerializer<JsonNull>(JsonNull.serializer(), JsonNull)
    private val noExplicitNullsOrDefaultsJson = Json {
        explicitNulls = false
        encodeDefaults = false
    }

    @Test
    fun testNullableJsonElementDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonElementSerializer::class)
            val value: JsonElement? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", noExplicitNullsOrDefaultsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }

    @Test
    fun testNullableJsonPrimitiveDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonPrimitiveSerializer::class)
            val value: JsonPrimitive? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", noExplicitNullsOrDefaultsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }

    @Test
    fun testNullableJsonNullDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonNullSerializer::class)
            val value: JsonNull? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", noExplicitNullsOrDefaultsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }
}
