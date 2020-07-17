/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlin.test.*

class DynamicToLongTest {

    @Serializable
    data class HasLong(val l: Long)

    private fun test(dynamic: dynamic, expectedResult: Result<Long>) {
        val parsed = kotlin.runCatching { Json.decodeFromDynamic(HasLong.serializer(), dynamic).l }
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
        shouldFail(js("{l: NaN}"))
        shouldFail(js("""{l: "a string"}"""))
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
