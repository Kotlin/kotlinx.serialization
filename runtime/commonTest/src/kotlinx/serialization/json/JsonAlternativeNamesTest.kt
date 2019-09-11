/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class WithNames(@JsonAlternativeNames(["foo", "_foo"]) val data: String)

@Serializable
data class WithDuplicateNames(val foo: String, @JsonAlternativeNames(["foo", "_foo"]) val data: String)

@Serializable
data class WithDuplicateNames2(@JsonAlternativeNames(["foo", "_foo"]) val data: String, val foo: String)

class JsonAlternativeNamesTest : JsonTestBase() {
    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""
    val json = Json(JsonConfiguration(strictMode = false, supportAlternateNames = true))

    @Test
    fun parsesAllAlternativeNames() {
        val data1 = json.parse(WithNames.serializer(), inputString1)
        assertEquals("foo", data1.data)
        val data2 = json.parse(WithNames.serializer(), inputString2)
        assertEquals("foo", data2.data)
    }

    @Test
    fun parseAllAlternativeNamesTree() {
        val data1 = json.parse(WithNames.serializer(), inputString1, useStreaming = false)
        assertEquals("foo", data1.data)
        val data2 = json.parse(WithNames.serializer(), inputString2, useStreaming = false)
        assertEquals("foo", data2.data)
    }

    @Test
    fun throwsAnErrorOnDuplicateNames() {
        val errorMessage =
            """Suggested name 'foo' for property data is already one of the names for property foo in kotlinx.serialization.json.WithDuplicateNames(data: kotlin.String, foo: kotlin.String)"""
        assertFailsWithMessage<IllegalStateException>(errorMessage) {
            json.parse(WithDuplicateNames.serializer(), inputString1, useStreaming = true)
        }
        assertFailsWithMessage<IllegalStateException>(errorMessage) {
            json.parse(WithDuplicateNames.serializer(), inputString1, useStreaming = false)
        }
    }

    @Test
    fun throwsAnErrorOnDuplicateNames2() {
        val errorMessage =
            """Suggested name 'foo' for property foo is already one of the names for property data in kotlinx.serialization.json.WithDuplicateNames2(data: kotlin.String, foo: kotlin.String)"""
        assertFailsWithMessage<IllegalStateException>(errorMessage) {
            json.parse(WithDuplicateNames2.serializer(), inputString1, useStreaming = true)
        }
        assertFailsWithMessage<IllegalStateException>(errorMessage) {
            json.parse(WithDuplicateNames2.serializer(), inputString1, useStreaming = false)
        }
    }
}
