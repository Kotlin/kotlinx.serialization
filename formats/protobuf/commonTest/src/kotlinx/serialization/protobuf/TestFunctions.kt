/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.reflect.KClass
import kotlin.test.*

fun <T> testConversion(data: T, serializer: KSerializer<T>, expectedHexString: String) {
    val string = ProtoBuf.encodeToHexString(serializer, data).uppercase()
    assertEquals(expectedHexString, string)
    assertEquals(data, ProtoBuf.decodeFromHexString(serializer, string))
}

inline fun <reified T> testConversion(data: T, expectedHexString: String) {
    val string = ProtoBuf.encodeToHexString(data).uppercase()
    assertEquals(expectedHexString, string)
    assertEquals(data, ProtoBuf.decodeFromHexString(string))
}

inline fun <reified T : Throwable> assertFailsWithMessage(
    message: String,
    assertionMessage: String? = null,
    block: () -> Unit
) {
    assertFailsWith<T>(
        assertionMessage,
        {
            assertFailsWith(message)
        },
        block,
    )
}

@DslMarker
annotation class ExceptionCheckDsl

@ExceptionCheckDsl
interface ExceptionCheckScope<T> {
    fun assertFailsWith(vararg message: String)
    fun <R : Throwable> assertCausedBy(byType: KClass<R>, assertion: ExceptionCheckScope<R>.() -> Unit)
}

@ExceptionCheckDsl
inline fun <reified R : Throwable> ExceptionCheckScope<*>.assertCausedBy(noinline assertion: ExceptionCheckScope<R>.() -> Unit) {
    assertCausedBy(R::class, assertion)
}

inline fun <reified T : Throwable> assertFailsWith(
    assertionMessage: String? = null,
    assertion: ExceptionCheckScope<T>.() -> Unit = {},
    block: () -> Unit
) {
    val exception = assertFailsWith(T::class, assertionMessage, block = block)
    val scope = buildExceptionCheckScope(exception)
    scope.assertion()
}

fun <T : Throwable> buildExceptionCheckScope(exception: T, depth: Int = 0): ExceptionCheckScope<T> =
    object : ExceptionCheckScope<T> {
        override fun assertFailsWith(vararg message: String) {
            val exceptionStackSize = exception.exceptionStackSize
            assertTrue(
                message.size <= exceptionStackSize,
                "Expected exception to be assembled by at least ${message.size} throwable(s), but it has $exceptionStackSize, actual exception is $exception."
            )
            var index = 0
            var currentException: Throwable? = exception
            while (index < message.size) {
                val currentMessage = message[index]
                assertNotNull(
                    currentException,
                    "Expected exception to have a cause with message $currentMessage, but it was null."
                )
                assertEquals(
                    currentMessage,
                    currentException.message,
                    "Exception messages are different at cause stack ${index + depth}."
                )
                val nextException = currentException.cause
                currentException = nextException
                index++
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <R : Throwable> assertCausedBy(byType: KClass<R>, assertion: ExceptionCheckScope<R>.() -> Unit) {
            val cause = exception.cause
            assertNotNull(cause, "Expected exception to have a cause of type $byType, but it was null.")
            assertEquals(
                byType,
                cause::class,
                "Current exception is caused by unexpected exception at cause stack $depth."
            )
            buildExceptionCheckScope<R>(cause as R, depth + 1).assertion()
        }

    }

private val Throwable.exceptionStackSize: Int
    get() {
        var count = 1
        var current = this
        while (current.cause != null) {
            count++
            current = current.cause!!
        }
        return count
    }