/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonPathTest : JsonTestBase() {

    @Serializable
    class Outer(val a: Int, val i: Inner)

    @Serializable
    class Inner(val a: Int, val b: String, val c: List<String>, val d: Map<Int, Box>)

    @Serializable
    class Box(val s: String)

    @Test
    fun testBasicError() = parametrizedTest { mode ->
        expectPath("$.a") { Json.decodeFromString<Outer>("""{"a":foo}""", mode) }
        expectPath("$.i") { Json.decodeFromString<Outer>("""{"a":42, "i":[]}""", mode) }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"a":43, "b":42}}""", mode) }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"b":42}}""", mode) }
    }

    @Test
    fun testMissingKey() = parametrizedTest { mode ->
        expectPath("$.i.d['1']") { Json.decodeFromString<Outer>("""{"a":42, "i":{"d":{"1":{}}}}""", mode) }
    }

    @Test
    fun testUnknownKeyIsProperlyReported() = parametrizedTest { mode ->
        expectPath("$.i") { Json.decodeFromString<Outer>("""{"a":42, "i":{"foo":42}}""", mode) }
        expectPath("$") { Json.decodeFromString<Outer>("""{"x":{}, "a": 42}""", mode) }
        expectPath("$") { Json.decodeFromString<Outer>("""{"a":42, "x":{}}""") }
    }

    @Test
    fun testMalformedRootObject() = parametrizedTest { mode ->
        expectPath("$") { Json.decodeFromString<Outer>("""{{""", mode) }
    }

    @Test
    fun testArrayIndex() = parametrizedTest { mode ->
        expectPath("$.i.c[1]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": ["a", 2]}}""", mode) }
        expectPath("$[2]") { Json.decodeFromString<List<String>>("""["a", "2", 3]""", mode) }
    }

    @Test
    fun testArrayIndexMalformedArray() = parametrizedTest { mode ->
        expectPath("$.i.c[0]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": [[]]}}""", mode) }
        expectPath("$[0]") { Json.decodeFromString<List<String>>("""[[]]""", mode) }
        // But we can here
        expectPath("$.i.c\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": {}}}""", mode) }
        expectPath("$\n") { Json.decodeFromString<List<String>>("""{""", mode) }
    }

    @Test
    fun testMapKey() = parametrizedTest { mode ->
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"foo": {}}}}""", mode) }
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"42": {"s":"s"}, "42.0":{}}}}""", mode) }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""{"foo":"bar"}""", mode) }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""{"42":"bar", "foo":"bar"}""", mode) }
        expectPath("$['42']['foo']") { Json.decodeFromString<Map<Int, Map<String, Int>>>("""{"42": {"foo":"bar"}}""", mode) }
    }

    @Test
    fun testMalformedMap() = parametrizedTest { mode ->
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": []}}""", mode) }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""[]""", mode) }
    }

    @Test
    fun testMapValue() = parametrizedTest { mode ->
        expectPath("$.i.d['42']\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"42": {"xx":"bar"}}}}""", mode) }
        expectPath("$.i.d['43']\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"42": {"s":"s"}, "43": {"xx":"bar"}}}}""", mode) }
        expectPath("$['239']") { Json.decodeFromString<Map<Int, String>>("""{"239":bar}""", mode) }
    }

    @Serializable
    class Fp(val d: Double)

    @Test
    fun testInvalidFp() = parametrizedTest { mode ->
        expectPath("$.d") { Json.decodeFromString<Fp>("""{"d": NaN}""", mode) }
    }

    @Serializable
    class EH(val e: E)
    enum class E

    @Test
    fun testUnknownEnum() = parametrizedTest { mode ->
        expectPath("$.e") { Json.decodeFromString<EH>("""{"e": "foo"}""", mode) }
    }

    @Serializable
    @SerialName("f")
    sealed class Sealed {

        @Serializable
        @SerialName("n")
        data class Nesting(val f: Sealed) : Sealed()

        @Serializable
        @SerialName("b")
        data class Box(val s: String) : Sealed()

        @Serializable
        @SerialName("d")
        data class DoubleNesting(val f: Sealed, val f2: Sealed) : Sealed()
    }

    @Test
    fun testHugeNestingToCheckResize() {
        parametrizedTest { mode ->
            val json = Json { useArrayPolymorphism = true }
            var outer = Sealed.Nesting(Sealed.Box("value"))
            repeat(100) {
                outer = Sealed.Nesting(outer)
            }
            val str = json.encodeToString(Sealed.serializer(), outer)
            // check that data is correctly formed
            assertEquals(outer, json.decodeFromString(Sealed.serializer(), str, mode))

            val malformed = str.replace("\"value\"", "42")
            val expectedPath = "$" + ".value.f".repeat(101) + ".value.s"
            expectPath(expectedPath) { json.decodeFromString(Sealed.serializer(), malformed, mode) }
        }
    }

    @Test
    fun testDoubleNestingNoArrayPoly() {
        parametrizedTest { mode ->
            val json = Json { useArrayPolymorphism = false }
            var outer1 = Sealed.Nesting(Sealed.Box("correct"))
            repeat(64) {
                outer1 = Sealed.Nesting(outer1)
            }

            var outer2 = Sealed.Nesting(Sealed.Box("incorrect"))
            repeat(33) {
                outer2 = Sealed.Nesting(outer2)
            }

            val value = Sealed.DoubleNesting(outer1, outer2)
            val str = json.encodeToString(Sealed.serializer(), value)
            // check that data is correctly formed
            assertEquals(value, json.decodeFromString(Sealed.serializer(), str, mode))

            val malformed = str.replace("\"incorrect\"", "42")
            val expectedPath = "$.f2" + ".f".repeat(34) + ".s"
            expectPath(expectedPath) { json.decodeFromString(Sealed.serializer(), malformed, mode) }
        }
    }

    @Serializable
    data class SimpleNested(val n: SimpleNested? = null, val t: DataObject? = null)

    @Serializable
    data object DataObject

    @Test
    fun testMalformedDataObjectInDeeplyNestedStructure() = parametrizedTest { mode ->
        var outer = SimpleNested(t = DataObject)
        repeat(20) {
            outer = SimpleNested(n = outer)
        }
        val str = Json.encodeToString(SimpleNested.serializer(), outer)
        // check that data is correctly formed
        assertEquals(outer, Json.decodeFromString(SimpleNested.serializer(), str, mode))

        val malformed = str.replace("{}", "42")
        val expectedPath = "$" + ".n".repeat(20) + ".t\n"
        expectPath(expectedPath) { Json.decodeFromString(SimpleNested.serializer(), malformed, mode) }
    }

    private inline fun expectPath(path: String, block: () -> Any?) {
        val message = runCatching { block() }
            .exceptionOrNull()!!.message!!
        assertContains(message, path)
    }
}
