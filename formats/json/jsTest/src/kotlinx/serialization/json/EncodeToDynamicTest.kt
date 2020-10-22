/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.test.*

@Suppress("UnsafeCastFromDynamic")
class EncodeToDynamicTest {
    @Serializable
    data class Data(val a: Int)

    @Serializable
    open class DataWrapper(open val s: String, val d: Data? = Data(1)) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class.js != other::class.js) return false

            other as DataWrapper

            if (s != other.s) return false
            if (d != other.d) return false

            return true
        }

        override fun hashCode(): Int {
            var result = s.hashCode()
            result = 31 * result + (d?.hashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class NestedList(val a: String, val list: List<Int>)

    @Serializable
    data class ListOfLists(val l: List<List<Data>>)

    @Serializable
    data class MapWrapper(val m: Map<String?, Int>)

    @Serializable
    data class ComplexMapWrapper(val m: Map<String, Data>)

    @Serializable
    data class WithChar(val a: Char)

    @Serializable
    data class WithLong(val l: Long)

    @Serializable
    data class AllTypes(
        val b: Byte,
        val s: Short,
        val i: Int,
        val f: Float,
        val d: Double,
        val c: Char,
        val B: Boolean,
        val S: String
    )

    @Serializable
    data class EnumWrapper(val e: Color)

    @Serializable
    sealed class Sealed {
        @Serializable
        data class One(val string: String) : Sealed()
    }

    @Serializable
    class WithJsName(@JsName("b") val a: String)

    @Serializable
    data class WithSerialName(@SerialName("b") val a: String)

    @Serializable
    enum class Color {
        RED,
        GREEN,

        @SerialName("red")
        WITH_SERIALNAME_red
    }

    @Serializable
    data class MyFancyClass(val value: String) {

        @Serializer(forClass = MyFancyClass::class)
        companion object : KSerializer<MyFancyClass> {

            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MyFancyClass", PrimitiveKind.STRING)
            override fun serialize(encoder: Encoder, value: MyFancyClass) {
                encoder.encodeString("fancy ${value.value}")
            }

            override fun deserialize(decoder: Decoder): MyFancyClass {
                return MyFancyClass(decoder.decodeString().removePrefix("fancy "))
            }
        }
    }

    @Test
    fun dynamicSimpleTest() {
        assertDynamicForm(Data(42)) { data, serialized ->
            assertEquals(data.a, serialized.a)
        }

        assertDynamicForm(WithChar('c')) { data, serialized ->
            assertEquals(data.a.toString(), serialized.a)
        }

        assertDynamicForm(AllTypes(1, 2, 3, 4.0f, 5.0, 'c', true, "string"))


        assertDynamicForm(WithLong(5L))
        assertDynamicForm(WithLong(MAX_SAFE_INTEGER.toLong()))
        assertDynamicForm(WithLong(MAX_SAFE_INTEGER.unaryMinus().toLong()))
    }

    @Test
    fun wrappedObjectsTest() {
        assertDynamicForm(DataWrapper("a string", Data(42))) { data, serialized ->
            assertEquals(data.s, serialized.s)
            assertNotNull(serialized.d)
            assertEquals(data.d?.a, serialized.d.a)
        }
    }

    // todo: this is a test for internal class. Rewrite it after implementing 'omitNulls' JSON flag.
    @Test
    @Ignore
    fun nullsTest() {
//        val data = DataWrapper("a string", null)
//
//        val serialized = DynamicObjectSerializer(
//            Json,
//            encodeNullAsUndefined = true
//        ).serialize(DataWrapper.serializer(), data)
//        assertNull(serialized.d)
//        assertFalse(js("""Object.keys(serialized).includes("d")"""), "should omit null properties")
//
//        val serializedWithNull = DynamicObjectSerializer(
//            Json,
//            encodeNullAsUndefined = false
//        ).serialize(DataWrapper.serializer(), data)
//        assertNull(serializedWithNull.d)
//        assertTrue(js("""Object.keys(serializedWithNull).includes("d")"""), "should contain null properties")
    }

    @Test
    fun listTest() {
        assertDynamicForm(listOf(1, 2, 3, 44), serializer = ListSerializer(Int.serializer())) { data, serialized ->
            assertNotNull(serialized.length, "length property should exist")
            assertEquals(data.size, serialized.length)

            for (i in data.indices) {
                assertEquals(data[i], serialized[i])
            }
        }
    }

    @Test
    fun arrayTest() {
        assertDynamicForm(intArrayOf(1, 2, 3, 44), serializer = IntArraySerializer(), true) { data, serialized ->
            assertNotNull(serialized.length, "length property should exist")
            assertEquals(data.size, serialized.length)

            for (i in data.indices) {
                assertEquals(data[i], serialized[i])
            }
        }
    }

    @Test
    fun nestedListTest() {
        assertDynamicForm(NestedList("a string", listOf(1, 2, 3, 44))) { data, serialized ->
            assertEquals(data.a, serialized.a)
            assertNotNull(serialized.list.length, "length property should exist")
            assertEquals(data.list.size, serialized.list.length)

            for (i in data.list.indices) {
                assertEquals(data.list[i], serialized.list[i])
            }
        }

    }

    @Test
    fun complexMapWrapperTest() {
        assertDynamicForm(ComplexMapWrapper(mapOf("key1" to Data(1), "key2" to Data(2))))
    }

    @Test
    fun mapWrapperTest() {
        assertDynamicForm(MapWrapper(mapOf("key1" to 1, "key2" to 2)))
    }

    @Test
    fun listOfListsTest() {
        assertDynamicForm(
            ListOfLists(
                listOf(
                    listOf(Data(11), Data(12), Data(13)),
                    listOf(Data(21), Data(22))
                )
            )
        ) { data, serialized ->
            assertEquals(data.l.size, serialized.l.length)
            assertEquals(data.l.first().size, serialized.l[0].length)
        }
    }

    @Test
    fun nestedCollections() {
        @Serializable
        data class NestedCollections(val data: Map<String, Map<String, List<Int>>>)

        assertDynamicForm(
            NestedCollections(
                mapOf(
                    "one" to mapOf("oneone" to listOf(11, 12, 13), "onetwo" to listOf(1)),
                    "two" to mapOf("twotwo" to listOf(22, 23))
                )
            )
            , serializer = NestedCollections.serializer()
        )
    }

    @Test
    fun enums() {
        assertDynamicForm(EnumWrapper(Color.RED))
        assertDynamicForm(Color.GREEN)
        assertDynamicForm(Color.WITH_SERIALNAME_red) { _, serialized ->
            assertEquals("red", serialized)
        }
    }

    @Test
    fun singlePrimitiveValue() {
        assertDynamicForm("some string")
        assertDynamicForm(1.toByte())
        assertDynamicForm(1.toShort())
        assertDynamicForm(1)
        assertDynamicForm(1.toFloat())
        assertDynamicForm(1.toDouble())
        assertDynamicForm('c')
        assertDynamicForm(true)
        assertDynamicForm(false)
        assertDynamicForm(1L)
        val result = Json.encodeToDynamic(String.serializer().nullable, null)
        assertEquals(null, result)
    }

    @Test
    fun sealed() {
        // test of sealed class but not polymorphic serialization
        assertDynamicForm(Sealed.One("one"))
    }

    @Test
    fun withSerialNam() {
        assertDynamicForm(WithSerialName("something")) { data, serialized ->
            assertEquals(data.a, serialized.b)
        }
    }

    @Test
    fun mapWithNullKey() {
        val serialized = Json.encodeToDynamic(
            MapSerializer(String.serializer().nullable, Int.serializer()),
            mapOf(null to 0, "a" to 1)
        )
        assertNotNull(serialized[null], "null key should be present in output")
    }

    @Test
    fun mapWithSimpleKey() {

        inline fun <reified T> assertSimpleMapForm(key: T, value: String) {
            assertDynamicForm(mapOf(key to value), MapSerializer(serializer(), String.serializer()))
        }

        assertSimpleMapForm(1, "Int 1")
        assertSimpleMapForm("s", "String s")
        assertSimpleMapForm('c', "char c")
        assertSimpleMapForm(2.toByte(), "Byte 2")
        assertSimpleMapForm(3.toShort(), "Short 3")
        assertSimpleMapForm(4.toLong(), "Long 4")
        assertSimpleMapForm(5.toFloat(), "Float 5")
        assertSimpleMapForm(6.toDouble(), "Double 6")

        assertDynamicForm(
            mapOf(
                Color.RED to "RED",
                Color.GREEN to "GREEN",
                Color.WITH_SERIALNAME_red to "red"
            ),
            MapSerializer(Color.serializer(), String.serializer())
        ) { _, serialized ->
            assertNotNull(serialized["red"], "WITH_SERIALNAME_red should be serialized as 'red'")
        }
    }

    @Test
    fun mapWithIllegalKey() {

        val exception = assertFails {
            Json.encodeToDynamic(
                MapSerializer(Data.serializer(), String.serializer()),
                mapOf(
                    Data(1) to "data",
                    Data(2) to "data",
                    Data(3) to "data"
                )
            )
        }
        assertEquals(IllegalArgumentException::class, exception::class)
        assertTrue("should have a helpful error message") {
            exception.message?.contains("can't be used in json as map key") == true
        }

        assertFails {
            @Suppress("CAST_NEVER_SUCCEEDS")
            assertDynamicForm(
                mapOf(
                    (null as? Data) to "Data null"
                ),
                MapSerializer(Data.serializer().nullable, String.serializer())
            )
        }


        val doubleSerializer = MapSerializer(Double.serializer(), String.serializer())
        val value = mapOf(0.5 to "0.5")
        var ex = assertFails {
            assertDynamicForm(value, doubleSerializer)
        }
        assertTrue("should have a helpful error message") {
            ex.message?.contains("can't be used in json as map key") == true
        }

        ex = assertFails {
            assertDynamicForm(mapOf(Double.NaN to "NaN"), doubleSerializer)
        }
        assertTrue("should have a helpful error message") {
            ex.message?.contains("can't be used in json as map key") == true
        }

        ex = assertFails {
            assertDynamicForm(mapOf(Double.NEGATIVE_INFINITY to "NaN"), doubleSerializer)
        }
        assertTrue("should have a helpful error message") {
            ex.message?.contains("can't be used in json as map key") == true
        }

        assertDynamicForm(mapOf(11.0 to "11"), doubleSerializer)

    }

    @Test
    fun customSerializerTest() {
        assertDynamicForm(MyFancyClass("apple"), MyFancyClass.serializer()) { _, serialized ->
            assertEquals("fancy apple", serialized)
        }

        assertDynamicForm(
            mapOf(MyFancyClass("apple") to "value"),
            MapSerializer(MyFancyClass.serializer(), String.serializer())
        ) { _, serialized ->
            assertNotNull(serialized["fancy apple"], "should contain key 'fancy apple'")
        }
    }
}

public inline fun <reified T : Any> assertDynamicForm(
    data: T,
    serializer: KSerializer<T> = EmptySerializersModule.serializer(),
    skipEqualsCheck:Boolean = false,
    noinline assertions: ((T, dynamic) -> Unit)? = null
) {
    val effectiveSerializer = serializer ?: EmptySerializersModule.serializer<T>()
    val serialized = Json.encodeToDynamic(effectiveSerializer, data)
    assertions?.invoke(data, serialized)
    val string = Json.encodeToString(effectiveSerializer, data)
    assertEquals(
        string,
        JSON.stringify(serialized),
        "JSON.stringify representation must be the same"
    )

    if (skipEqualsCheck) return // arrays etc.
    assertEquals(data, Json.decodeFromString<T>(effectiveSerializer, string))
}
