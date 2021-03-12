/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ReplaceArrayOfWithLiteral") // https://youtrack.jetbrains.com/issue/KT-22578

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

@Serializable
data class WithNames(@JsonNames(arrayOf("foo", "_foo")) val data: String)

@Serializable
data class CollisionWithPrimary(val foo: String, @JsonNames(arrayOf("foo", "_foo")) val data: String)

@Serializable
data class CollisionWithAlternate(
    @JsonNames(arrayOf("_foo")) val data: String,
    @JsonNames(arrayOf("_foo")) val foo: String
)

class JsonAlternativeNamesTest : JsonTestBase() {
    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""
    private val json = Json { useAlternativeNames = true }

    @Test
    fun parsesAllAlternativeNames() {
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
            assertFailsWithMessage<IllegalStateException>(
                expectedErrorMessage,
                "Class ${serializer.descriptor.serialName} did not fail with streaming=$streaming"
            ) {
                json.decodeFromString(serializer, input, useStreaming = streaming)
            }
        }

    @Test
    fun throwsAnErrorOnDuplicateNames() = doThrowTest(
        """The suggested name 'foo' for property data is already one of the names for property foo in kotlinx.serialization.features.CollisionWithPrimary(foo: kotlin.String, data: kotlin.String)""",
        CollisionWithPrimary.serializer(),
        inputString1
    )

    @Test
    fun throwsAnErrorOnDuplicateNames2() = doThrowTest(
        """The suggested name '_foo' for property foo is already one of the names for property data in kotlinx.serialization.features.CollisionWithAlternate(data: kotlin.String, foo: kotlin.String)""",
        CollisionWithAlternate.serializer(),
        inputString2
    )
}
