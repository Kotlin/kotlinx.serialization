/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SkipDefaultsTest {
    private val json = Json { encodeDefaults = false }

    @Serializable
    data class Data(val bar: String, val foo: Int = 42) {
        var list: List<Int> = emptyList()
        val listWithSomething: List<Int> = listOf(1, 2, 3)
    }

    @Test
    fun serializeCorrectlyDefaults() {
        val jsonWithDefaults = Json { encodeDefaults = true }
        val d = Data("bar")
        assertEquals(
            """{"bar":"bar","foo":42,"list":[],"listWithSomething":[1,2,3]}""",
            jsonWithDefaults.encodeToString(Data.serializer(), d)
        )
    }

    @Test
    fun serializeCorrectly() {
        val d = Data("bar", 100).apply { list = listOf(1, 2, 3) }
        assertEquals("""{"bar":"bar","foo":100,"list":[1,2,3]}""", json.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropBody() {
        val d = Data("bar", 43)
        assertEquals("""{"bar":"bar","foo":43}""", json.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropAll() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar"}""", json.encodeToString(Data.serializer(), d))
    }

}
