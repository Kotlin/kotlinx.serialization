/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*
import kotlin.test.assertFailsWith

class DecodeFromDynamicTest {
    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class DataWrapper(val s: String, val d: Data?)

    @Serializable
    data class DataWrapperOptional(val s: String,val d: Data? = null)

    @Serializable
    data class IntList(val l: List<Int>)

    @Serializable
    data class ListOfLists(val l: List<List<Data>>)

    @Serializable
    data class MapWrapper(val m: Map<String, Int>)

    @Serializable
    data class ComplexMapWrapper(val m: Map<String, Data>)

    @Serializable
    data class IntMapWrapper(val m: Map<Int, Int>)

    @Serializable
    data class WithChar(val a: Char)

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
    data class NonTrivialMap(val m: Map<String, Char>)

    data class NotDefault(val a: Int)

    object NDSerializer : KSerializer<NotDefault> {
        override val descriptor = buildClassSerialDescriptor("notDefault") {
            element<Int>("a")
        }

        override fun serialize(encoder: Encoder, value: NotDefault) {
            encoder.encodeInt(value.a)
        }

        override fun deserialize(decoder: Decoder): NotDefault {
            return NotDefault(decoder.decodeInt())
        }
    }

    @Serializable
    data class NDWrapper(@Contextual val data: NotDefault)

    @Test
    fun dynamicSimpleTest() {
        val dyn = js("{a: 42}")
        val parsed = Json.decodeFromDynamic(Data.serializer(), dyn)
        assertEquals(Data(42), parsed)

        val dyn2 = js("{a: 'a'}")
        val parsed2 = Json.decodeFromDynamic(WithChar.serializer(), dyn2)
        assertEquals(WithChar('a'), parsed2)

        val dyn3 = js("{a: 97}")
        val parsed3 = Json.decodeFromDynamic(WithChar.serializer(), dyn3)
        assertEquals(WithChar('a'), parsed3)
    }

    @Test
    fun dynamicAllTypesTest() {
        val dyn = js("""{ b: 1, s: 2, i: 3, f: 1.0, d: 42.0, c: 'a', B: true, S: "str"}""")
        val kotlinObj = AllTypes(1, 2, 3, 1.0f, 42.0, 'a', true, "str")

        assertEquals(kotlinObj, Json.decodeFromDynamic(AllTypes.serializer(), dyn))
    }

    @Test
    fun dynamicNestedTest() {
        val dyn = js("""{s:"foo", d:{a:42}}""")
        val parsed = Json.decodeFromDynamic(DataWrapper.serializer(), dyn)
        val expected = DataWrapper("foo", Data(42))
        assertEquals(expected, parsed)
        assertEquals(3, parsed.s.length)
        assertFailsWith(ClassCastException::class) { dyn as DataWrapper }
    }

    @Test
    fun dynamicNullableTest() {
        val dyn1 = js("""({s:"foo", d: null})""")
        val dyn2 = js("""({s:"foo"})""")
        val dyn3 = js("""({s:"foo", d: undefined})""")

        assertEquals(DataWrapper("foo", null), Json.decodeFromDynamic(DataWrapper.serializer(), dyn1))
        assertFailsWithMissingField {
            Json.decodeFromDynamic(
                DataWrapper.serializer(),
                dyn2
            )
        }
        assertFailsWithMissingField {
            Json.decodeFromDynamic(
                DataWrapper.serializer(),
                dyn3
            )
        }
    }

    @Test
    fun dynamicOptionalTest() {
        val dyn1 = js("""({s:"foo", d: null})""")
        val dyn2 = js("""({s:"foo"})""")
        val dyn3 = js("""({s:"foo", d: undefined})""")

        assertEquals(
            DataWrapperOptional("foo", null),
            Json.decodeFromDynamic(DataWrapperOptional.serializer(), dyn1)
        )
        assertEquals(
            DataWrapperOptional("foo", null),
            Json.decodeFromDynamic(DataWrapperOptional.serializer(), dyn2)
        )
        assertEquals(
            DataWrapperOptional("foo", null),
            Json.decodeFromDynamic(DataWrapperOptional.serializer(), dyn3)
        )
    }

    @Test
    fun dynamicListTest() {
        val dyn1 = js("""({l:[1,2]})""")
        val dyn2 = js("""({l:[[],[{a:42}]]})""")

        assertEquals(IntList(listOf(1, 2)), Json.decodeFromDynamic(IntList.serializer(), dyn1))
        assertEquals(
            ListOfLists(listOf(listOf(), listOf(Data(42)))),
            Json.decodeFromDynamic(ListOfLists.serializer(), dyn2)
        )
    }

    @Test
    fun dynamicMapTest() {
        val dyn = js("({m : {\"a\": 1, \"b\" : 2}})")
        val m = MapWrapper(mapOf("a" to 1, "b" to 2))
        assertEquals(m, Json.decodeFromDynamic(MapWrapper.serializer(), dyn))
    }

    @Test
    fun testFunnyMap() {
        val dyn = js("({m: {\"a\": 'b', \"b\" : 'a'}})")
        val m = NonTrivialMap(mapOf("a" to 'b', "b" to 'a'))
        assertEquals(m, Json.decodeFromDynamic(NonTrivialMap.serializer(), dyn))
    }

    @Test
    fun dynamicMapComplexTest() {
        val dyn = js("({m: {1: {a: 42}, 2: {a: 43}}})")
        val m = ComplexMapWrapper(mapOf("1" to Data(42), "2" to Data(43)))
        assertEquals(m, Json.decodeFromDynamic(ComplexMapWrapper.serializer(), dyn))
    }

    @Test
    fun testIntMapTest() {
        val dyn = js("({m: {1: 2, 3: 4}})")
        val m = IntMapWrapper(mapOf(1 to 2, 3 to 4))
        assertEquals(m, Json.decodeFromDynamic(IntMapWrapper.serializer(), dyn))
    }

    @Test
    fun parseWithCustomSerializers() {
        val json = Json { serializersModule = serializersModuleOf(NotDefault::class, NDSerializer) }
        val dyn1 = js("({data: 42})")
        assertEquals(NDWrapper(NotDefault(42)),
            json.decodeFromDynamic(NDWrapper.serializer(), dyn1)
        )
    }

}
