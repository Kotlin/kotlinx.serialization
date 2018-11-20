/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:ContextualSerialization(CustomSerializersTest.B::class)
package kotlinx.serialization

import kotlinx.serialization.context.*
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomSerializersTest {
    @Serializable
    data class A(@SerialId(1) val b: B)

    data class B(@SerialId(1) val value: Int)

    object BSerializer : KSerializer<B> {
        override fun serialize(encoder: Encoder, obj: B) {
            encoder.encodeInt(obj.value)
        }

        override fun deserialize(decoder: Decoder): B {
            return B(decoder.decodeInt())
        }

        override val descriptor: SerialDescriptor = SerialClassDescImpl("B")
    }

    @Serializable
    data class BList(@SerialId(1) val bs: List<B>)

    @Serializable
    data class C(@SerialId(1) @Optional val a: Int = 31, @SerialId(2) val b: Int = 42) {
        @Serializer(forClass = C::class)
        companion object: KSerializer<C> {
            override fun serialize(encoder: Encoder, obj: C) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, obj.b)
                if (obj.a != 31) elemOutput.encodeIntElement(descriptor, 0, obj.a)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList1(@SerialId(1) val c: List<C>)

    @Serializable
    data class CList2(@SerialId(1) @Optional val d: Int = 5, @SerialId(2) val c: List<C>) {
        @Serializer(forClass = CList2::class)
        companion object: KSerializer<CList2> {
            override fun serialize(encoder: Encoder, obj: CList2) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeSerializableElement(descriptor, 1, C.list, obj.c)
                if (obj.d != 5) elemOutput.encodeIntElement(descriptor, 0, obj.d)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList3(@SerialId(1) @Optional val e: List<C> = emptyList(), @SerialId(2) val f: Int) {
        @Serializer(forClass = CList3::class)
        companion object: KSerializer<CList3> {
            override fun serialize(encoder: Encoder, obj: CList3) {
                val elemOutput = encoder.beginStructure(descriptor)
                if (obj.e.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, obj.e)
                elemOutput.encodeIntElement(descriptor, 1, obj.f)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList4(@SerialId(1) @Optional val g: List<C> = emptyList(), @SerialId(2) val h: Int) {
        @Serializer(forClass = CList4::class)
        companion object: KSerializer<CList4> {
            override fun serialize(encoder: Encoder, obj: CList4) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, obj.h)
                if (obj.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, obj.g)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList5(@SerialId(1) @Optional val g: List<Int> = emptyList(), @SerialId(2) val h: Int) {
        @Serializer(forClass = CList5::class)
        companion object: KSerializer<CList5> {
            override fun serialize(encoder: Encoder, obj: CList5) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, obj.h)
                if (obj.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, IntSerializer.list,
                                                                                 obj.g)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    val moduleWithB = object : SerialModule {
        override fun registerIn(context: MutableSerialContext) {
            context.registerSerializer(B::class, BSerializer)
        }
    }

    private fun createJsonWithB() = Json(unquoted = true).apply { install(moduleWithB) }

    @Test
    fun writeCustom() {
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.stringify(a)
        assertEquals("{b:2}", s)
    }

    @Test
    fun readCustom() {
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.parse<A>("{b:2}")
        assertEquals(a, s)
    }

    @Test
    fun writeCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val s = j.stringify(obj)
        assertEquals("{bs:[1,2,3]}", s)
    }

    @Test
    fun readCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val bs = j.parse<BList>("{bs:[1,2,3]}")
        assertEquals(obj, bs)
    }

    @Test
    fun writeCustomListRootLevel() {
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val s = j.stringify(BSerializer.list, obj)
        assertEquals("[1,2,3]", s)
    }

    @Test
    fun readCustomListRootLevel() {
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val bs = j.parse(BSerializer.list, "[1,2,3]")
        assertEquals(obj, bs)
    }

    @Test
    fun writeCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = Json(unquoted = true)
        val s = j.stringify(obj)
        assertEquals("{b:2,a:1}", s)
    }

    @Test
    fun writeCustomOmitDefault() {
        val obj = C(b = 2)
        val j = Json(unquoted = true)
        val s = j.stringify(obj)
        assertEquals("{b:2}", s)
    }

    @Test
    fun readCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = Json(unquoted = true)
        val s = j.parse<C>("{b:2,a:1}")
        assertEquals(obj, s)
    }

    @Test
    fun readCustomOmitDefault() {
        val obj = C(b = 2)
        val j = Json(unquoted = true)
        val s = j.parse<C>("{b:2}")
        assertEquals(obj, s)
    }

    @Test
    fun writeListOfOptional() {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val s = Json(unquoted = true).stringify(C.list, obj)
        assertEquals("[{b:42,a:1},{b:2},{b:4,a:3}]", s)
    }

    @Test
    fun readListOfOptional() {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val j = "[{b:42,a:1},{b:2},{b:4,a:3}]"
        val s = Json(unquoted = true).parse(C.list, j)
        assertEquals(obj, s)
    }

    @Test
    fun writeOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun writeOptionalList1Quoted() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = Json(unquoted = false).stringify(obj)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun readOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = "{c:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}", s)
    }

    @Test
    fun readOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}]}", s)
    }

    @Test
    fun readOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}]}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}", s)
    }

    @Test
    fun readOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = "{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList3b() {
        val obj = CList3(f=99)
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{f:99}", s)
    }

    @Test
    fun readOptionalList3b() {
        val obj = CList3(f=99)
        val j = "{f:99}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun readOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = "{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList4b() {
        val obj = CList4(h=97)
        val j = "{h:97}"
        val s = Json(unquoted = true).stringify(obj)
        assertEquals(j, s)
    }

    @Test
    fun readOptionalList4b() {
        val obj = CList4(h=97)
        val j = "{h:97}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList5a() {
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{h:5,g:[9,8,7,6,5]}", s)
    }

    @Test
    fun readOptionalList5a() {
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val j = "{h:5,g:[9,8,7,6,5]}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList5b() {
        val obj = CList5(h=999)
        val s = Json(unquoted = true).stringify(obj)
        assertEquals("{h:999}", s)
    }

    @Test
    fun readOptionalList5b() {
        val obj = CList5(h=999)
        val j = "{h:999}"
        assertEquals(obj, Json(unquoted = true).parse(j))
    }

    @Test
    fun mapBuiltinsTest() {
        val map = mapOf(1 to "1", 2 to "2")
        val serial = (IntSerializer to StringSerializer).map
        val s = Json.unquoted.stringify(serial, map)
        assertEquals("{1:1,2:2}", s)
    }

    @Test
    fun resolveAtRootLevel() {
        val j = createJsonWithB()
        val bs = j.parse<B>("1")
        assertEquals(B(1), bs)
    }
}
