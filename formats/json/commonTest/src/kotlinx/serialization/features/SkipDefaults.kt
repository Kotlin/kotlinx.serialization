/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.EncodeDefault.Mode.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SkipDefaultsTest {
    private val jsonDropDefaults = Json { encodeDefaults = false }
    private val jsonEncodeDefaults = Json { encodeDefaults = true }

    @Serializable
    data class Data(val bar: String, val foo: Int = 42) {
        var list: List<Int> = emptyList()
        val listWithSomething: List<Int> = listOf(1, 2, 3)
    }

    @Serializable
    data class DifferentModes(
        val a: String = "a",
        @EncodeDefault val b: String = "b",
        @EncodeDefault(ALWAYS) val c: String = "c",
        @EncodeDefault(NEVER) val d: String = "d"
    )

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
        assertEquals(
            """{"bar":"bar","foo":100,"list":[1,2,3]}""",
            jsonDropDefaults.encodeToString(Data.serializer(), d)
        )
    }

    @Test
    fun serializeCorrectlyAndDropBody() {
        val d = Data("bar", 43)
        assertEquals("""{"bar":"bar","foo":43}""", jsonDropDefaults.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropAll() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar"}""", jsonDropDefaults.encodeToString(Data.serializer(), d))
    }

    @Test
    fun encodeDefaultsAnnotationWithFlag() {
        val data = DifferentModes()
        assertEquals("""{"a":"a","b":"b","c":"c"}""", jsonEncodeDefaults.encodeToString(data))
        assertEquals("""{"b":"b","c":"c"}""", jsonDropDefaults.encodeToString(data))
    }

}
