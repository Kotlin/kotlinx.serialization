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

package kotlinx.serialization

import kotlinx.serialization.json.JSON
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializationCasesTest {
    @Test
    fun testConstructorValProperties() {
        @Serializable
        data class Data(val a: Int, val b: Int)
        val data = Data(1, 2)

        // Serialize with internal serializer for Data class
        assertEquals("{a:1,b:2}", JSON.unquoted.stringify(data))
        assertEquals(data, JSON.parse<Data>("{a:1,b:2}"))

        // Serialize with external serializer for Data class
        @Serializer(forClass=Data::class)
        class ExtDataSerializer
        assertEquals("{a:1,b:2}", JSON.unquoted.stringify(ExtDataSerializer(), data))
        assertEquals(data, JSON.parse(ExtDataSerializer(), "{a:1,b:2}"))
    }

    @Test
    fun testBodyVarProperties() {
        @Serializable
        class Data {
            var a = 0
            var b = 0
            override fun equals(other: Any?) = other is Data && other.a == a && other.b == b
        }
        val data = Data().apply {
            a = 1
            b = 2
        }

        // Serialize with internal serializer for Data class
        assertEquals("{a:1,b:2}", JSON.unquoted.stringify(data))
        assertEquals(data, JSON.parse<Data>("{a:1,b:2}"))

        // Serialize with external serializer for Data class
        @Serializer(forClass=Data::class)
        class ExtDataSerializer
        assertEquals("{a:1,b:2}", JSON.unquoted.stringify(ExtDataSerializer(), data))
        assertEquals(data, JSON.parse(ExtDataSerializer(), "{a:1,b:2}"))
    }

    enum class TintEnum { LIGHT, DARK }

    @Test
    fun testNestedValues() {
        @Serializable
        data class Data(
                val a: String,
                val b: List<Int>,
                val c: Map<String, TintEnum>
        )
        val data = Data("Str", listOf(1, 2), mapOf("lt" to TintEnum.LIGHT, "dk" to TintEnum.DARK))

        // Serialize with internal serializer for Data class
        assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", JSON.unquoted.stringify(data))
        assertEquals(data, JSON.parse<Data>("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))

        // Serialize with external serializer for Data class
        @Serializer(forClass=Data::class)
        class ExtDataSerializer
        assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", JSON.unquoted.stringify(ExtDataSerializer(), data))
        assertEquals(data, JSON.parse(ExtDataSerializer(), "{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))
    }

}
