/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonPrettyPrintTest : JsonTestBase() {
    val fmt = Json(default) { prettyPrint = true; encodeDefaults = true }

    @Serializable
    class Empty

    @Serializable
    class A(val empty: Empty = Empty())

    @Serializable
    class B(val prefix: String = "a", val empty: Empty = Empty(), val postfix: String = "b")

    @Serializable
    class Recursive(val rec: Recursive?, val empty: Empty = Empty())

    @Serializable
    class WithListRec(val rec: WithListRec?, val l: List<Int> = listOf())

    @Serializable
    class WithDefaults(val x: String = "x", val y: Int = 0)

    @Test
    fun testTopLevel() = parametrizedTest { mode ->
        assertEquals("{}", fmt.encodeToString(Empty(), mode))
    }

    @Test
    fun testWithDefaults() = parametrizedTest { mode ->
        val dropDefaults = Json(fmt) { encodeDefaults = false }
        val s = "{\n    \"boxed\": {}\n}"
        assertEquals(s, dropDefaults.encodeToString(Box(WithDefaults()), mode))
    }

    @Test
    fun testPlain() = parametrizedTest { mode ->
        val s = """{
        |    "empty": {}
        |}""".trimMargin()
        assertEquals(s, fmt.encodeToString(A(), mode))
    }

    @Test
    fun testInside() = parametrizedTest { mode ->
        val s = """{
        |    "prefix": "a",
        |    "empty": {},
        |    "postfix": "b"
        |}""".trimMargin()
        assertEquals(s, fmt.encodeToString(B(), mode))
    }

    @Test
    fun testRecursive() = parametrizedTest { mode ->
        val obj = Recursive(Recursive(null))
        val s = "{\n    \"rec\": {\n        \"rec\": null,\n        \"empty\": {}\n    },\n    \"empty\": {}\n}"
        assertEquals(s, fmt.encodeToString(obj, mode))
    }

    @Test
    fun test() = parametrizedTest { mode ->
        val obj = WithListRec(WithListRec(null), listOf(1, 2, 3))
        val s =
            "{\n    \"rec\": {\n        \"rec\": null,\n        \"l\": []\n    },\n    \"l\": [\n        1,\n        2,\n        3\n    ]\n}"
        assertEquals(s, fmt.encodeToString(obj, mode))
    }
}
