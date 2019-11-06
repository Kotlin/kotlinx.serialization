/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ReplaceArrayOfWithLiteral") // https://youtrack.jetbrains.com/issue/KT-22578

package kotlinx.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class WithNames(@JsonAlternativeNames(arrayOf("foo", "_foo")) val data: String)

@Serializable
data class WithDuplicateNames(val foo: String, @JsonAlternativeNames(arrayOf("foo", "_foo")) val data: String)

@Serializable
data class WithDuplicateNames2(@JsonAlternativeNames(arrayOf("foo", "_foo")) val data: String, val foo: String)

class JsonAlternativeNamesTest : JsonTestBase() {
    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""
    private val json = Json(JsonConfiguration(strictMode = false, supportAlternativeNames = true))

    @Test
    fun parsesAllAlternativeNames() {
        for (input in listOf(inputString1, inputString2)) {
            for (streaming in listOf(true, false)) {
                val data = json.parse(WithNames.serializer(), input, useStreaming = streaming)
                assertEquals("foo", data.data, "Failed to parse input '$input' with streaming=$streaming")
            }
        }
    }

    private fun <T> doThrowTest(expectedErrorMessage: String, serializer: KSerializer<T>) =
        parametrizedTest { streaming ->
            assertFailsWithMessage<IllegalStateException>(
                expectedErrorMessage,
                "Class ${serializer.descriptor.name} did not fail with streaming=$streaming"
            ) {
                json.parse(serializer, inputString1, useStreaming = streaming)
            }
        }

    @Test
    fun throwsAnErrorOnDuplicateNames() = doThrowTest(
        """The suggested name 'foo' for property data is already one of the names for property foo in kotlinx.serialization.json.WithDuplicateNames(foo: kotlin.String, data: kotlin.String)""",
        WithDuplicateNames.serializer()
    )

    @Test
    fun throwsAnErrorOnDuplicateNames2() = doThrowTest(
        """The suggested name 'foo' for property foo is already one of the names for property data in kotlinx.serialization.json.WithDuplicateNames2(data: kotlin.String, foo: kotlin.String)""",
        WithDuplicateNames2.serializer()
    )
}
