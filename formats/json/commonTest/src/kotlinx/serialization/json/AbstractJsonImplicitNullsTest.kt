package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

@Ignore
abstract class AbstractJsonImplicitNullsTest {
    @Serializable
    data class Nullable(
        val f0: Int?,
        val f1: Int?,
        val f2: Int?,
        val f3: Int?,
    )

    @Serializable
    data class WithNotNull(
        val f0: Int?,
        val f1: Int?,
        val f2: Int,
    )

    @Serializable
    data class WithOptional(
        val f0: Int?,
        val f1: Int? = 1,
        val f2: Int = 2,
    )

    @Serializable
    data class Outer(val i: Inner)

    @Serializable
    data class Inner(val s1: String?, val s2: String?)

    @Serializable
    data class ListWithNullable(val l: List<Int?>)

    @Serializable
    data class MapWithNullable(val m: Map<Int?, Int?>)

    @Serializable
    data class NullableList(val l: List<Int>?)

    @Serializable
    data class NullableMap(val m: Map<Int, Int>?)


    private val format = Json { explicitNulls = false }

    protected abstract fun <T> Json.encode(value: T, serializer: KSerializer<T>): String

    protected abstract fun <T> Json.decode(json: String, serializer: KSerializer<T>): T

    @Test
    fun testExplicit() {
        val plain = Nullable(null, 10, null, null)
        val json = """{"f0":null,"f1":10,"f2":null,"f3":null}"""

        assertEquals(json, Json.encode(plain, Nullable.serializer()))
        assertEquals(plain, Json.decode(json, Nullable.serializer()))
    }

    @Test
    fun testNullable() {
        val plain = Nullable(null, 10, null, null)
        val json = """{"f1":10}"""

        assertEquals(json, format.encode(plain, Nullable.serializer()))
        assertEquals(plain, format.decode(json, Nullable.serializer()))
    }

    @Test
    fun testMissingNotNull() {
        val json = """{"f1":10}"""

        assertFailsWith(SerializationException::class) {
            format.decode(json, WithNotNull.serializer())
        }
    }

    @Test
    fun testDecodeOptional() {
        val json = """{}"""

        val decoded = format.decode(json, WithOptional.serializer())
        assertEquals(WithOptional(null), decoded)
    }


    @Test
    fun testNestedJsonObject() {
        val json = """{"i": {}}"""

        val decoded = format.decode(json, Outer.serializer())
        assertEquals(Outer(Inner(null, null)), decoded)
    }

    @Test
    fun testListWithNullable() {
        val jsonWithNull = """{"l":[null]}"""
        val jsonWithEmptyList = """{"l":[]}"""

        val encoded = format.encode(ListWithNullable(listOf(null)), ListWithNullable.serializer())
        assertEquals(jsonWithNull, encoded)

        val decoded = format.decode(jsonWithEmptyList, ListWithNullable.serializer())
        assertEquals(ListWithNullable(emptyList()), decoded)
    }

    @Test
    fun testMapWithNullable() {
        val jsonWithNull = """{"m":{null:null}}"""
        val jsonWithQuotedNull = """{"m":{"null":null}}"""
        val jsonWithEmptyList = """{"m":{}}"""

        val encoded = format.encode(MapWithNullable(mapOf(null to null)), MapWithNullable.serializer())
        //Json encode map null key as `null:` but other external utilities may encode it as a String `"null":`
        assertTrue { listOf(jsonWithNull, jsonWithQuotedNull).contains(encoded) }

        val decoded = format.decode(jsonWithEmptyList, MapWithNullable.serializer())
        assertEquals(MapWithNullable(emptyMap()), decoded)
    }

    @Test
    fun testNullableList() {
        val json = """{}"""

        val encoded = format.encode(NullableList(null), NullableList.serializer())
        assertEquals(json, encoded)

        val decoded = format.decode(json, NullableList.serializer())
        assertEquals(NullableList(null), decoded)
    }

    @Test
    fun testNullableMap() {
        val json = """{}"""

        val encoded = format.encode(NullableMap(null), NullableMap.serializer())
        assertEquals(json, encoded)

        val decoded = format.decode(json, NullableMap.serializer())
        assertEquals(NullableMap(null), decoded)
    }

}
