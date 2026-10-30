/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.Platform
import kotlinx.serialization.test.checkDecodingException
import kotlinx.serialization.test.currentPlatform
import kotlin.test.*

class LongAsStringTest : JsonTestBase() {
    @Serializable
    data class HasLong(@Serializable(LongAsStringSerializer::class) val l: Long)

    @Test
    fun canSerializeAsStringAndParseBack() = parametrizedTest { jsonTestingMode ->
        val original = HasLong(Long.MAX_VALUE - 1)
        val str = default.encodeToString(HasLong.serializer(), original, jsonTestingMode)
        assertEquals("""{"l":"9223372036854775806"}""", str)
        val restored = default.decodeFromString(HasLong.serializer(), str, jsonTestingMode)
        assertEquals(original, restored)
    }

    @Test
    fun canNotDeserializeInvalidString() = parametrizedTest { jsonTestingMode ->
        val str = """{"l": "this is definitely not a long"}"""
        checkDecodingException(
            jsonTestingMode,
            { default.decodeFromString(HasLong.serializer(), str, jsonTestingMode) },
            {
                assertContains(exception.message, "Deserialization failed because of")
                // NumberFormatException messages vary by platform
                assertContains(exception.message, "exception in the decoder")
                assertContains(exception.message, exception.shortMessage)
                path("$.l")
                input(str)
                cause<NumberFormatException>()
            })
        val str2 = """{"l": "1000000000000000000000"}""" // toooo long for Long
        checkDecodingException(
            jsonTestingMode,
            { default.decodeFromString(HasLong.serializer(), str2, jsonTestingMode) },
            {
                assertContains(exception.message, "Deserialization failed because of")
                // NumberFormatException messages vary by platform
                assertContains(exception.message, "exception in the decoder")
                assertContains(exception.message, exception.shortMessage)
                path("$.l")
                input(str2)
                cause<NumberFormatException>()
            })
    }
}
