/*
 *  Copyright 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomSerializersTest {
    @Serializable
    data class A(@SerialId(1) val b: B)

    data class B(@SerialId(1) val value: Int)

    object BSerializer : KSerializer<B> {
        override fun save(output: KOutput, obj: B) {
            output.writeIntValue(obj.value)
        }

        override fun load(input: KInput): B {
            return B(input.readIntValue())
        }

        override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("B")
    }

    @Serializable
    data class BList(@SerialId(1) val bs: List<B>)

    @Serializable
    data class C(@SerialId(1) @Optional val a: Int = 31, @SerialId(2) val b: Int = 42) {
        @Serializer(forClass = C::class)
        companion object {
            override fun save(output: KOutput, obj: C) {
                val elemOutput = output.writeBegin(serialClassDesc)
                elemOutput.writeIntElementValue(serialClassDesc, 1, obj.b)
                if (obj.a != 31) elemOutput.writeIntElementValue(serialClassDesc, 0, obj.a)
                elemOutput.writeEnd(serialClassDesc)
            }
        }
    }

    @Serializable
    data class CList1(@SerialId(1) val c: List<C>)

    @Serializable
    data class CList2(@SerialId(1) @Optional val d: Int = 5, @SerialId(2) val c: List<C>) {
        @Serializer(forClass = CList2::class)
        companion object {
            override fun save(output: KOutput, obj: CList2) {
                val elemOutput = output.writeBegin(serialClassDesc)
                elemOutput.writeSerializableElementValue(serialClassDesc, 1, C.list, obj.c)
                if (obj.d != 5) output.writeIntElementValue(serialClassDesc, 0, obj.d)
                elemOutput.writeEnd(serialClassDesc)
            }
        }
    }

    @Serializable
    data class CList3(@SerialId(1) @Optional val e: List<C> = emptyList(), @SerialId(2) val f: Int) {
        @Serializer(forClass = CList3::class)
        companion object {
            override fun save(output: KOutput, obj: CList3) {
                val elemOutput = output.writeBegin(serialClassDesc)
                if (obj.e.isNotEmpty()) elemOutput.writeSerializableElementValue(serialClassDesc, 0, C.list, obj.e)
                output.writeIntElementValue(serialClassDesc, 1, obj.f)
                elemOutput.writeEnd(serialClassDesc)
            }
        }
    }

    @Serializable
    data class CList4(@SerialId(1) @Optional val g: List<C> = emptyList(), @SerialId(2) val h: Int) {
        @Serializer(forClass = CList4::class)
        companion object {
            override fun save(output: KOutput, obj: CList4) {
                val elemOutput = output.writeBegin(serialClassDesc)
                output.writeIntElementValue(serialClassDesc, 1, obj.h)
                if (obj.g.isNotEmpty()) elemOutput.writeSerializableElementValue(serialClassDesc, 0, C.list, obj.g)
                elemOutput.writeEnd(serialClassDesc)
            }
        }
    }

    @Serializable
    data class CList5(@SerialId(1) @Optional val g: List<Int> = emptyList(), @SerialId(2) val h: Int) {
        @Serializer(forClass = CList5::class)
        companion object {
            override fun save(output: KOutput, obj: CList5) {
                val elemOutput = output.writeBegin(serialClassDesc)
                output.writeIntElementValue(serialClassDesc, 1, obj.h)
                if (obj.g.isNotEmpty()) elemOutput.writeSerializableElementValue(serialClassDesc, 0, IntSerializer.list,
                                                                                 obj.g)
                elemOutput.writeEnd(serialClassDesc)
            }
        }
    }


    @Test
    fun writeCustom() {
        val a = A(B(2))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val s = j.stringify(a)
        assertEquals("{b:2}", s)
    }

    @Test
    fun readCustom() {
        val a = A(B(2))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val s = j.parse<A>("{b:2}")
        assertEquals(a, s)
    }

    @Test
    fun writeCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val s = j.stringify(obj)
        assertEquals("{bs:[1,2,3]}", s)
    }

    @Test
    fun readCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val bs = j.parse<BList>("{bs:[1,2,3]}")
        assertEquals(obj, bs)
    }

    @Test
    fun writeCustomListRootLevel() {
        val obj = listOf(B(1), B(2), B(3))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val s = j.stringify(BSerializer.list, obj)
        assertEquals("[1,2,3]", s)
    }

    @Test
    fun readCustomListRootLevel() {
        val obj = listOf(B(1), B(2), B(3))
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val bs = j.parse(BSerializer.list, "[1,2,3]")
        assertEquals(obj, bs)
    }

    @Test
    fun writeCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = JSON(unquoted = true)
        val s = j.stringify(obj)
        assertEquals("{b:2,a:1}", s)
    }

    @Test
    fun writeCustomOmitDefault() {
        val obj = C(b = 2)
        val j = JSON(unquoted = true)
        val s = j.stringify(obj)
        assertEquals("{b:2}", s)
    }

    @Test
    fun readCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = JSON(unquoted = true)
        val s = j.parse<C>("{b:2,a:1}")
        assertEquals(obj, s)
    }

    @Test
    fun readCustomOmitDefault() {
        val obj = C(b = 2)
        val j = JSON(unquoted = true)
        val s = j.parse<C>("{b:2}")
        assertEquals(obj, s)
    }

    @Test
    fun writeListOfOptional() {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val s = JSON(unquoted = true).stringify(C.list, obj)
        assertEquals("[{b:42,a:1},{b:2},{b:4,a:3}]", s)
    }

    @Test
    fun readListOfOptional() {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val j = "[{b:42,a:1},{b:2},{b:4,a:3}]"
        val s = JSON(unquoted = true).parse(C.list, j)
        assertEquals(obj, s)
    }

    @Test
    fun writeOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun readOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = "{c:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}", s)
    }

    @Test
    fun readOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}]}", s)
    }

    @Test
    fun readOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}]}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}", s)
    }

    @Test
    fun readOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = "{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList3b() {
        val obj = CList3(f=99)
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{f:99}", s)
    }

    @Test
    fun readOptionalList3b() {
        val obj = CList3(f=99)
        val j = "{f:99}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun readOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = "{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList4b() {
        val obj = CList4(h=97)
        val j = "{h:97}"
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals(j, s)
    }

    @Test
    fun readOptionalList4b() {
        val obj = CList4(h=97)
        val j = "{h:97}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList5a() {
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{h:5,g:[9,8,7,6,5]}", s)
    }

    @Test
    fun readOptionalList5a() {
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val j = "{h:5,g:[9,8,7,6,5]}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun writeOptionalList5b() {
        val obj = CList5(h=999)
        val s = JSON(unquoted = true).stringify(obj)
        assertEquals("{h:999}", s)
    }

    @Test
    fun readOptionalList5b() {
        val obj = CList5(h=999)
        val j = "{h:999}"
        assertEquals(obj, JSON(unquoted = true).parse(j))
    }

    @Test
    fun mapBuiltinsTest() {
        val map = mapOf(1 to "1", 2 to "2")
        val serial = (IntSerializer to StringSerializer).map
        val s = JSON.unquoted.stringify(serial, map)
        assertEquals("{1:1,2:2}", s)
    }

    @Test
    fun resolveAtRootLevel() {
        val scope = SerialContext()
        scope.registerSerializer(B::class, BSerializer)
        val j = JSON(unquoted = true, context = scope)
        val bs = j.parse<B>("1")
        assertEquals(B(1), bs)
    }
}