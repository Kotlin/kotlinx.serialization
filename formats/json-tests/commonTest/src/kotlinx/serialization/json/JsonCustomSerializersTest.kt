/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseContextualSerialization(JsonCustomSerializersTest.B::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonCustomSerializersTest : JsonTestBase() {

    @Serializable
    data class A(@Id(1) val b: B)

    data class B(@Id(1) val value: Int)

    object BSerializer : KSerializer<B> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("B", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: B) {
            encoder.encodeInt(value.value)
        }

        override fun deserialize(decoder: Decoder): B {
            return B(decoder.decodeInt())
        }
    }

    @Serializable
    data class BList(@Id(1) val bs: List<B>)

    @Serializable(C.Companion::class)
    data class C(@Id(1) val a: Int = 31, @Id(2) val b: Int = 42) {
        @Serializer(forClass = C::class)
        companion object : KSerializer<C> {
            override fun serialize(encoder: Encoder, value: C) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.b)
                if (value.a != 31 || elemOutput.shouldEncodeElementDefault(descriptor, 0)) elemOutput.encodeIntElement(descriptor, 0, value.a)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList1(@Id(1) val c: List<C>)

    @Serializable(CList2.Companion::class)
    data class CList2(@Id(1) val d: Int = 5, @Id(2) val c: List<C>) {
        @Serializer(forClass = CList2::class)
        companion object : KSerializer<CList2> {
            override fun serialize(encoder: Encoder, value: CList2) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeSerializableElement(descriptor, 1, ListSerializer(C), value.c)
                if (value.d != 5 || elemOutput.shouldEncodeElementDefault(descriptor, 0)) {
                    elemOutput.encodeIntElement(descriptor, 0, value.d)
                }
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList3.Companion::class)
    data class CList3(@Id(1) val e: List<C> = emptyList(), @Id(2) val f: Int) {
        @Serializer(forClass = CList3::class)
        companion object : KSerializer<CList3> {
            override fun serialize(encoder: Encoder, value: CList3) {
                val elemOutput = encoder.beginStructure(descriptor)
                if (value.e.isNotEmpty() || elemOutput.shouldEncodeElementDefault(descriptor, 0)) {
                    elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.e)
                }
                elemOutput.encodeIntElement(descriptor, 1, value.f)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList4.Companion::class)
    data class CList4(@Id(1) val g: List<C> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList4::class)
        companion object : KSerializer<CList4> {
            override fun serialize(encoder: Encoder, value: CList4) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty() || elemOutput.shouldEncodeElementDefault(descriptor, 0)) {
                    elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.g)
                }
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList5.Companion::class)
    data class CList5(@Id(1) val g: List<Int> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList5::class)
        companion object : KSerializer<CList5> {
            override fun serialize(encoder: Encoder, value: CList5) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty()  || elemOutput.shouldEncodeElementDefault(descriptor, 0)) {
                    elemOutput.encodeSerializableElement(
                        descriptor, 0, ListSerializer(Int.serializer()),
                        value.g
                    )
                }
                elemOutput.endStructure(descriptor)
            }
        }
    }

    private val moduleWithB = serializersModuleOf(B::class, BSerializer)

    private fun createJsonWithB() = Json { isLenient = true; serializersModule = moduleWithB; useAlternativeNames = false }
    // useAlternativeNames uses SerialDescriptor.hashCode,
    // which is unavailable for partially-customized serializers such as in this file
    private val jsonNoAltNames = Json { useAlternativeNames = false }

    @Test
    fun testWriteCustom() = parametrizedTest { jsonTestingMode ->
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.encodeToString(a, jsonTestingMode)
        assertEquals("""{"b":2}""", s)
    }

    @Test
    fun testReadCustom() = parametrizedTest { jsonTestingMode ->
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.decodeFromString<A>("{b:2}", jsonTestingMode)
        assertEquals(a, s)
    }

    @Test
    fun testWriteCustomList() = parametrizedTest { jsonTestingMode ->
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val s = j.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"bs":[1,2,3]}""", s)
    }

    @Test
    fun testReadCustomList() = parametrizedTest { jsonTestingMode ->
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val bs = j.decodeFromString<BList>("{bs:[1,2,3]}", jsonTestingMode)
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomListRootLevel() = parametrizedTest { jsonTestingMode ->
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val s = j.encodeToString(ListSerializer(BSerializer), obj, jsonTestingMode)
        assertEquals("[1,2,3]", s)
    }

    @Test
    fun testReadCustomListRootLevel() = parametrizedTest { jsonTestingMode ->
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val bs = j.decodeFromString(ListSerializer(BSerializer), "[1,2,3]", jsonTestingMode)
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomInvertedOrder() = parametrizedTest { jsonTestingMode ->
        val obj = C(1, 2)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"b":2,"a":1}""", s)
    }

    @Test
    fun testWriteCustomOmitDefault() = parametrizedTest { jsonTestingMode ->
        val obj = C(b = 2)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"b":2}""", s)
    }

    @Test
    fun testReadCustomInvertedOrder() = parametrizedTest { jsonTestingMode ->
        val obj = C(1, 2)
        val s = jsonNoAltNames.decodeFromString<C>("""{"b":2,"a":1}""", jsonTestingMode)
        assertEquals(obj, s)
    }

    @Test
    fun testReadCustomOmitDefault() = parametrizedTest { jsonTestingMode ->
        val obj = C(b = 2)
        val s = jsonNoAltNames.decodeFromString<C>("""{"b":2}""", jsonTestingMode)
        assertEquals(obj, s)
    }

    @Test
    fun testWriteListOfOptional() = parametrizedTest { jsonTestingMode ->
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val s = jsonNoAltNames.encodeToString(ListSerializer(C), obj, jsonTestingMode)
        assertEquals("""[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]""", s)
    }

    @Test
    fun testReadListOfOptional() = parametrizedTest { jsonTestingMode ->
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val j = """[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]"""
        val s = jsonNoAltNames.decodeFromString(ListSerializer<C>(C), j, jsonTestingMode)
        assertEquals(obj, s)
    }

    @Test
    fun testWriteOptionalList1() = parametrizedTest { jsonTestingMode ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testWriteOptionalList1Quoted() = parametrizedTest { jsonTestingMode ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testReadOptionalList1() = parametrizedTest { jsonTestingMode ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = """{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList2a() = parametrizedTest { jsonTestingMode ->
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}],"d":7}""", s)
    }

    @Test
    fun testReadOptionalList2a() = parametrizedTest { jsonTestingMode ->
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = """{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}],"d":7}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList2b() = parametrizedTest { jsonTestingMode ->
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}]}""", s)
    }

    @Test
    fun testReadOptionalList2b() = parametrizedTest { jsonTestingMode ->
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = """{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}]}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList3a() = parametrizedTest { jsonTestingMode ->
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"e":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}],"f":99}""", s)
    }

    @Test
    fun testReadOptionalList3a() = parametrizedTest { jsonTestingMode ->
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = """{"e":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}],"f":99}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList3b() = parametrizedTest { jsonTestingMode ->
        val obj = CList3(f = 99)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"f":99}""", s)
    }

    @Test
    fun testReadOptionalList3b() = parametrizedTest { jsonTestingMode ->
        val obj = CList3(f = 99)
        val j = """{"f":99}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList4a() = parametrizedTest { jsonTestingMode ->
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"h":54,"g":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testReadOptionalList4a() = parametrizedTest { jsonTestingMode ->
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = """{"h":54,"g":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList4b() = parametrizedTest { jsonTestingMode ->
        val obj = CList4(h = 97)
        val j = """{"h":97}"""
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals(j, s)
    }

    @Test
    fun testReadOptionalList4b() = parametrizedTest { jsonTestingMode ->
        val obj = CList4(h = 97)
        val j = """{"h":97}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList5a() = parametrizedTest { jsonTestingMode ->
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"h":5,"g":[9,8,7,6,5]}""", s)
    }

    @Test
    fun testReadOptionalList5a() = parametrizedTest { jsonTestingMode ->
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val j = """{"h":5,"g":[9,8,7,6,5]}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testWriteOptionalList5b() = parametrizedTest { jsonTestingMode ->
        val obj = CList5(h = 999)
        val s = jsonNoAltNames.encodeToString(obj, jsonTestingMode)
        assertEquals("""{"h":999}""", s)
    }

    @Test
    fun testReadOptionalList5b() = parametrizedTest { jsonTestingMode ->
        val obj = CList5(h = 999)
        val j = """{"h":999}"""
        assertEquals(obj, jsonNoAltNames.decodeFromString(j, jsonTestingMode))
    }

    @Test
    fun testMapBuiltinsTest() = parametrizedTest { jsonTestingMode ->
        val map = mapOf(1 to "1", 2 to "2")
        val serial = MapSerializer(Int.serializer(), String.serializer())
        val s = jsonNoAltNames.encodeToString(serial, map, jsonTestingMode)
        assertEquals("""{"1":"1","2":"2"}""", s)
    }

    @Test
    fun testResolveAtRootLevel() = parametrizedTest { jsonTestingMode ->
        val j = createJsonWithB()
        val bs = j.decodeFromString<B>("1", jsonTestingMode)
        assertEquals(B(1), bs)
    }
}
