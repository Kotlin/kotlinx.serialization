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

package kotlinx.serialization

import kotlin.test.*

class DynamicToLongTest {

    @Serializable
    data class HasLong(val l: Long)

    private fun test(dynamic: dynamic, expectedResult: Result<Long>) {
        val parsed = kotlin.runCatching { DynamicObjectParser().parse(dynamic, HasLong.serializer()).l }
        assertEquals(expectedResult.isSuccess, parsed.isSuccess, "Results are different")
        parsed.onSuccess { assertEquals(expectedResult.getOrThrow(), it) }
        // to compare without message
        parsed.onFailure { assertSame(expectedResult.exceptionOrNull()!!::class, it::class) }
    }

    private fun shouldFail(dynamic: dynamic) = test(dynamic,  Result.failure(SerializationException("")))

    @Test
    fun canParseNotSoBigLongs() {
        test(js("{l:1}"), Result.success(1))
        test(js("{l:0}"), Result.success(0))
        test(js("{l:-1}"), Result.success(-1))
    }

    @Test
    fun ignoresIncorrectValues() {
        shouldFail(js("{l:0.5}"))
        shouldFail(js("{l: Math.PI}"))
        shouldFail(js("{l:Infinity}"))
        shouldFail(js("{l:+Infinity}"))
        shouldFail(js("{l:-Infinity}"))
    }

    @Test
    fun handlesEdgyValues() {
        test(js("{l:Number.MAX_SAFE_INTEGER}"), Result.success(MAX_SAFE_INTEGER.toLong()))
        test(js("{l:Number.MAX_SAFE_INTEGER - 1}"), Result.success(MAX_SAFE_INTEGER.toLong() - 1))
        test(js("{l:-Number.MAX_SAFE_INTEGER}"), Result.success(-MAX_SAFE_INTEGER.toLong()))
        shouldFail(js("{l: Number.MAX_SAFE_INTEGER + 1}"))
        shouldFail(js("{l: Number.MAX_SAFE_INTEGER + 2}"))
        shouldFail(js("{l: -Number.MAX_SAFE_INTEGER - 1}"))
        shouldFail(js("{l: 2e100}"))
        shouldFail(js("{l: 2e100 + 1}"))
        test(js("{l: Math.pow(2, 53) - 1}"), Result.success(MAX_SAFE_INTEGER.toLong()))
    }
}
