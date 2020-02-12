/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongAsStringSerializer
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.*

class LongAsStringTest : JsonTestBase() {
    @Serializable
    data class HasLong(@Serializable(LongAsStringSerializer::class) val l: Long)

    @Test
    fun canSerializeAsStringAndParseBack() = parametrizedTest { useStreaming ->
        val original = HasLong(Long.MAX_VALUE - 1)
        val str = default.stringify(HasLong.serializer(), original, useStreaming)
        assertEquals("""{"l":"9223372036854775806"}""", str)
        val restored = default.parse(HasLong.serializer(), str, useStreaming)
        assertEquals(original, restored)
    }

    @Test
    fun canNotDeserializeInvalidString() = parametrizedTest { useStreaming ->
        val str = """{"l": "this is definitely not a long"}"""
        assertFailsWith<NumberFormatException> { default.parse(HasLong.serializer(), str, useStreaming) }
        val str2 = """{"l": "1000000000000000000000"}""" // toooo long for Long
        assertFailsWith<NumberFormatException> { default.parse(HasLong.serializer(), str2, useStreaming) }
    }
}
