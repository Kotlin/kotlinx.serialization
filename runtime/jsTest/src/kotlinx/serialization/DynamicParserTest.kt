/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class DynamicParserTest {
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
        override val descriptor = SerialDescriptor("notDefault") {
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
    data class NDWrapper(@ContextualSerialization val data: NotDefault)

    @Serializable
    sealed class Sealed {
        @Serializable
        @SerialName("one")
        data class One(val string: String) : Sealed()
    }

    @Test
    fun dynamicSimpleTest() {
        val dyn = js("{a: 42}")
        val parsed = DynamicObjectParser().parse(dyn, Data.serializer())
        assertEquals(Data(42), parsed)

        val dyn2 = js("{a: 'a'}")
        val parsed2 = DynamicObjectParser().parse(dyn2, WithChar.serializer())
        assertEquals(WithChar('a'), parsed2)

        val dyn3 = js("{a: 97}")
        val parsed3 = DynamicObjectParser().parse(dyn3, WithChar.serializer())
        assertEquals(WithChar('a'), parsed3)
    }

    @Test
    fun dynamicAllTypesTest() {
        val dyn = js("""{ b: 1, s: 2, i: 3, f: 1.0, d: 42.0, c: 'a', B: true, S: "str"}""")
        val kotlinObj = AllTypes(1, 2, 3, 1.0f, 42.0, 'a', true, "str")

        assertEquals(kotlinObj, DynamicObjectParser().parse(dyn, AllTypes.serializer()))
    }

    @Test
    fun dynamicNestedTest() {
        val dyn = js("""{s:"foo", d:{a:42}}""")
        val parsed = DynamicObjectParser().parse<DataWrapper>(dyn, DataWrapper.serializer())
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

        assertEquals(DataWrapper("foo", null), DynamicObjectParser().parse<DataWrapper>(dyn1, DataWrapper.serializer()))
        assertFailsWith(MissingFieldException::class) {
            DynamicObjectParser().parse<DataWrapper>(
                dyn2,
                DataWrapper.serializer()
            )
        }
        assertFailsWith(MissingFieldException::class) {
            DynamicObjectParser().parse<DataWrapper>(
                dyn3,
                DataWrapper.serializer()
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
            DynamicObjectParser().parse<DataWrapperOptional>(dyn1, DataWrapperOptional.serializer())
        )
        assertEquals(
            DataWrapperOptional("foo", null),
            DynamicObjectParser().parse<DataWrapperOptional>(dyn2, DataWrapperOptional.serializer())
        )
        assertEquals(
            DataWrapperOptional("foo", null),
            DynamicObjectParser().parse<DataWrapperOptional>(dyn3, DataWrapperOptional.serializer())
        )
    }

    @Test
    fun dynamicListTest() {
        val dyn1 = js("""({l:[1,2]})""")
        val dyn2 = js("""({l:[[],[{a:42}]]})""")

        assertEquals(IntList(listOf(1, 2)), DynamicObjectParser().parse<IntList>(dyn1, IntList.serializer()))
        assertEquals(
            ListOfLists(listOf(listOf(), listOf(Data(42)))),
            DynamicObjectParser().parse<ListOfLists>(dyn2, ListOfLists.serializer())
        )
    }

    @Test
    fun dynamicMapTest() {
        val dyn = js("({m : {\"a\": 1, \"b\" : 2}})")
        val m = MapWrapper(mapOf("a" to 1, "b" to 2))
        assertEquals(m, DynamicObjectParser().parse<MapWrapper>(dyn, MapWrapper.serializer()))
    }

    @Test
    fun testFunnyMap() {
        val dyn = js("({m : {\"a\": 'b', \"b\" : 'a'}})")
        val m = NonTrivialMap(mapOf("a" to 'b', "b" to 'a'))
        assertEquals(m, DynamicObjectParser().parse(dyn, NonTrivialMap.serializer()))
    }

    @Test
    fun dynamicMapComplexTest() {
        val dyn = js("({m : {1: {a: 42}, 2: {a: 43}}})")
        val m = ComplexMapWrapper(mapOf("1" to Data(42), "2" to Data(43)))
        assertEquals(m, DynamicObjectParser().parse<ComplexMapWrapper>(dyn, ComplexMapWrapper.serializer()))
    }

    @Test
    fun parseWithCustomSerializers() {
        val deserializer = DynamicObjectParser(serializersModule = serializersModuleOf(NotDefault::class, NDSerializer))
        val dyn1 = js("({data: 42})")
        assertEquals(NDWrapper(NotDefault(42)), deserializer.parse(dyn1, NDWrapper.serializer()))
    }

    @Test
    @Ignore
    fun parsePolymorphicDefault() {
        // TODO object-based polymorphic deserialization requires additional special handling
        //  because the discriminator lives inside the same object which is also being decoded.

        val dyn = js("""({type:"one",string:"value"})""")
        val expected = Sealed.One("value")

        val actual1 = DynamicObjectParser().parse(dyn, Sealed.serializer())
        assertEquals(expected, actual1)

        val p = DynamicObjectParser(configuration = JsonConfiguration())
        val actual2 = p.parse(dyn, Sealed.serializer())
        assertEquals(expected, actual2)
    }

    @Test
    fun parsePolymorphicArray() {
        val dyn = js("""(["one",{"string":"value"}])""")
        val expected = Sealed.One("value")

        val p = DynamicObjectParser(configuration = JsonConfiguration(useArrayPolymorphism = true))
        val actual = p.parse(dyn, Sealed.serializer())
        assertEquals(expected, actual)
    }
}
