/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonPathTest : JsonTestBase() {

    @Serializable
    class Outer(val a: Int, val i: Inner)

    @Serializable
    class Inner(val a: Int, val b: String, val c: List<String>, val d: Map<Int, Box>)

    @Serializable
    class Box(val s: String)

    @Test
    fun testBasicError() {
        expectPath("$.a") { Json.decodeFromString<Outer>("""{"a":foo}""") }
        expectPath("$.i") { Json.decodeFromString<Outer>("""{"a":42, "i":[]}""") }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"a":43, "b":42}""") }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"b":42}""") }
    }

    @Test
    fun testMissingKey() {
        expectPath("$.i.d['1']") { Json.decodeFromString<Outer>("""{"a":42, "i":{"d":{1:{}}""") }
    }

    @Test
    fun testUnknownKeyIsProperlyReported() {
        expectPath("$.i") { Json.decodeFromString<Outer>("""{"a":42, "i":{"foo":42}""") }
        expectPath("$") { Json.decodeFromString<Outer>("""{"x":{}, "a": 42}""") }
        // The only place we have misattribution in
        // Json.decodeFromString<Outer>("""{"a":42, "x":{}}""")
    }

    @Test
    fun testMalformedRootObject() {
        expectPath("$") { Json.decodeFromString<Outer>("""{{""") }
    }

    @Test
    fun testArrayIndex() {
        expectPath("$.i.c[1]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": ["a", 2]}""") }
        expectPath("$[2]") { Json.decodeFromString<List<String>>("""["a", "2", 3]""") }
    }

    @Test
    fun testArrayIndexMalformedArray() {
        // Also zeroes as we cannot distinguish what exactly wen wrong is such cases
        expectPath("$.i.c[0]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": [[""") }
        expectPath("$[0]") { Json.decodeFromString<List<String>>("""[[""") }
        // But we can here
        expectPath("$.i.c\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": {}}}""") }
        expectPath("$\n") { Json.decodeFromString<List<String>>("""{""") }
    }

    @Test
    fun testMapKey() {
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"foo": {}}""") }
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"s":"s"}, 42.0:{}}""") }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""{"foo":"bar"}""") }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""{42:"bar", "foo":"bar"}""") }
        expectPath("$['42']['foo']") { Json.decodeFromString<Map<Int, Map<String, Int>>>("""{42: {"foo":"bar"}""") }
    }

    @Test
    fun testMalformedMap() {
        expectPath("$.i.d\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": []""") }
        expectPath("$\n") { Json.decodeFromString<Map<Int, String>>("""[]""") }
    }

    @Test
    fun testMapValue() {
        expectPath("$.i.d['42']\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"xx":"bar"}}""") }
        expectPath("$.i.d['43']\n") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"s":"s"}, 43: {"xx":"bar"}}}""") }
        expectPath("$['239']") { Json.decodeFromString<Map<Int, String>>("""{239:bar}""") }
    }

    @Serializable
    class Fp(val d: Double)

    @Test
    fun testInvalidFp() {
        expectPath("$.d") { Json.decodeFromString<Fp>("""{"d": NaN}""") }
    }

    @Serializable
    class EH(val e: E)
    enum class E

    @Test
    fun testUnknownEnum() {
        expectPath("$.e") { Json.decodeFromString<EH>("""{"e": "foo"}""") }
    }

    @Serializable
    @SerialName("f")
    sealed class Sealed {

        @Serializable
        @SerialName("n")
        class Nesting(val f: Sealed) : Sealed()

        @Serializable
        @SerialName("b")
        class Box(val s: String) : Sealed()

        @Serializable
        @SerialName("d")
        class DoubleNesting(val f: Sealed, val f2: Sealed) : Sealed()
    }

    // TODO use non-array polymorphism when https://github.com/Kotlin/kotlinx.serialization/issues/1839 is fixed
    @Test
    fun testHugeNestingToCheckResize() = jvmOnly {
        val json = Json { useArrayPolymorphism = true }
        var outer = Sealed.Nesting(Sealed.Box("value"))
        repeat(100) {
            outer = Sealed.Nesting(outer)
        }
        val str = json.encodeToString(Sealed.serializer(), outer)
        // throw-away data
        json.decodeFromString(Sealed.serializer(), str)

        val malformed = str.replace("\"value\"", "42")
        val expectedPath = "$" + ".value.f".repeat(101) + ".value.s"
        expectPath(expectedPath) { json.decodeFromString(Sealed.serializer(), malformed) }
    }

    @Test
    fun testDoubleNesting() = jvmOnly {
        val json = Json { useArrayPolymorphism = true }
        var outer1 = Sealed.Nesting(Sealed.Box("correct"))
        repeat(64) {
            outer1 = Sealed.Nesting(outer1)
        }

        var outer2 = Sealed.Nesting(Sealed.Box("incorrect"))
        repeat(33) {
            outer2 = Sealed.Nesting(outer2)
        }

        val str = json.encodeToString(Sealed.serializer(), Sealed.DoubleNesting(outer1, outer2))
        // throw-away data
        json.decodeFromString(Sealed.serializer(), str)

        val malformed = str.replace("\"incorrect\"", "42")
        val expectedPath = "$.value.f2" + ".value.f".repeat(34) + ".value.s"
        expectPath(expectedPath) { json.decodeFromString(Sealed.serializer(), malformed) }
    }

    private inline fun expectPath(path: String, block: () -> Unit) {
        val message = runCatching { block() }
            .exceptionOrNull()!!.message!!
        assertContains(message, path)
    }
}
