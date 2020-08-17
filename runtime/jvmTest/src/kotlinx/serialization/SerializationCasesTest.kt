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

import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*

class SerializationCasesTest : JsonTestBase() {

    @Serializable
    data class Data1(val a: Int, val b: Int)

    @Serializer(forClass = Data1::class)
    object ExtDataSerializer1

    @Test
    fun testConstructorValProperties() {
        val data = Data1(1, 2)

        // Serialize with internal serializer for Data class
        assertEquals("""{"a":1,"b":2}""", default.encodeToString(data))
        assertEquals(data, Json.decodeFromString<Data1>("""{"a":1,"b":2}"""))

        // Serialize with external serializer for Data class
        assertEquals("""{"a":1,"b":2}""", default.encodeToString(ExtDataSerializer1, data))
        assertEquals(data, Json.decodeFromString(ExtDataSerializer1, """{"a":1,"b":2}"""))
    }

    @Serializable
    class Data2 {
        var a = 0
        var b = 0
        override fun equals(other: Any?) = other is Data2 && other.a == a && other.b == b
    }

    @Serializer(forClass=Data2::class)
    object ExtDataSerializer2

    @Test
    fun testBodyVarProperties() {
        val data = Data2().apply {
            a = 1
            b = 2
        }

        // Serialize with internal serializer for Data class
        assertEquals("""{"a":1,"b":2}""", default.encodeToString(data))
        assertEquals(data, Json.decodeFromString<Data2>("""{"a":1,"b":2}"""))

        // Serialize with external serializer for Data class
        assertEquals("""{"a":1,"b":2}""", default.encodeToString(ExtDataSerializer2, data))
        assertEquals(data, Json.decodeFromString(ExtDataSerializer2, """{"a":1,"b":2}"""))
    }

    enum class TintEnum { LIGHT, DARK }

    @Serializable
    data class Data3(
        val a: String,
        val b: List<Int>,
        val c: Map<String, TintEnum>
    )

    // Serialize with external serializer for Data class
    @Serializer(forClass = Data3::class)
    object ExtDataSerializer3

    @Test
    fun testNestedValues() {
        val data = Data3("Str", listOf(1, 2), mapOf("lt" to TintEnum.LIGHT, "dk" to TintEnum.DARK))
        // Serialize with internal serializer for Data class
        val expected = """{"a":"Str","b":[1,2],"c":{"lt":"LIGHT","dk":"DARK"}}"""
        assertEquals(expected, default.encodeToString(data))
        assertEquals(data, Json.decodeFromString<Data3>(expected))
        assertEquals(expected, default.encodeToString(ExtDataSerializer3, data))
        assertEquals(data, Json.decodeFromString(ExtDataSerializer3, expected))
    }
}
