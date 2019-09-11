/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ReplaceArrayOfWithLiteral") // https://youtrack.jetbrains.com/issue/KT-22578

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonAlternativeNamesTest : JsonTestBase() {

    @Serializable
    data class WithNames(@JsonNames(arrayOf("foo", "_foo")) val data: String)

    @Serializable
    data class CollisionWithAlternate(
        @JsonNames(arrayOf("_foo")) val data: String,
        @JsonNames(arrayOf("_foo")) val foo: String
    )

    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""
    private val json = Json { useAlternativeNames = true }

    @Test
    fun testParsesAllAlternativeNames() {
        for (input in listOf(inputString1, inputString2)) {
            for (streaming in listOf(true, false)) {
                val data = json.decodeFromString(WithNames.serializer(), input, useStreaming = streaming)
                assertEquals("foo", data.data, "Failed to parse input '$input' with streaming=$streaming")
            }
        }
    }

    private fun <T> doThrowTest(
        expectedErrorMessage: String,
        serializer: KSerializer<T>,
        input: String
    ) =
        parametrizedTest { streaming ->
            assertFailsWithMessage<SerializationException>(
                expectedErrorMessage,
                "Class ${serializer.descriptor.serialName} did not fail with streaming=$streaming"
            ) {
                json.decodeFromString(serializer, input, useStreaming = streaming)
            }
        }

    @Test
    fun testThrowsAnErrorOnDuplicateNames2() = doThrowTest(
        """The suggested name '_foo' for property foo is already one of the names for property data""",
        CollisionWithAlternate.serializer(),
        inputString2
    )
}
