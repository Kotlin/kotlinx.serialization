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
import org.junit.Test
import kotlin.test.assertEquals

class CustomSerializersTest {
    @Serializable
    data class A(val b: B)

    data class B(val value: Int)

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
    data class BList(val bs: List<B>)

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
    fun mapBuiltinsTest() {
        val map = mapOf(1 to "1", 2 to "2")
        val serial = (IntSerializer to StringSerializer).map
        val s = JSON.unquoted.stringify(serial, map)
        assertEquals("{1:1,2:2}",s)
    }
}