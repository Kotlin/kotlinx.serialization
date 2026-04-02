/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonEncodingException
import kotlinx.serialization.json.JsonTestingMode
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EncodingExceptionAsserter(val mode: JsonTestingMode, val exception: JsonEncodingException) {
    var hasHint = false
    var hasSerialName = false

    fun message(msg: String, alternativeForTree: String? = null) {
        val expected = alternativeForTree.takeIf { mode == JsonTestingMode.TREE } ?: msg
        assertContains(exception.message, expected, message = "Full message is incomplete")
        assertContains(
            expected,
            exception.shortMessage,
            message = "Short message is not substring of expected message"
        )
    }

    fun serialName(name: String?) {
        assertEquals(name,  exception.classSerialName, message = "Serial name is not equal to expected serial name")
        if (name != null) hasSerialName = true
    }

    fun hint(hint: String) {
        assertContains(exception.message, hint, message = "Hint is not substring of expected message")
        assertContains(exception.hint!!, hint, message = "Hint is not substring of expected hint")
        hasHint = true
    }

    fun assertMissing() {
        if (!hasHint) assertNull(exception.hint, "Hint is not null")
        if (!hasSerialName) assertNull(exception.classSerialName, "Serial name is not null")
    }
}

inline fun checkEncodingException(
    mode: JsonTestingMode,
    action: () -> Any?,
    assertions: EncodingExceptionAsserter.() -> Unit
) {
    val e = assertFailsWith(JsonEncodingException::class, action)
    val asserter = EncodingExceptionAsserter(mode, e)
    val result= runCatching {
        asserter.assertions()
        asserter.assertMissing()
    }
    if (result.isFailure) {
        println("Test failed for exception: ${asserter.exception.message}")
        asserter.exception.printStackTrace() // cannot do initCause outside of JVM
        throw result.exceptionOrNull()!!
    }
}


class DecodingExceptionAsserter(val mode: JsonTestingMode, val exception: JsonDecodingException) {
    private var hasPath = false
    private var hasOffset = false
    private var hasHint = false

    fun message(msg: String, alternativeForTree: String? = null) {
        val expected = alternativeForTree.takeIf { mode == JsonTestingMode.TREE } ?: msg
        assertContains(exception.message, expected, message = "Full message is incomplete")
        assertContains(
            expected,
            exception.shortMessage,
            message = "Short message is not substring of expected message"
        )
    }

    fun path(path: String) {
        assertContains(exception.message, " at path: $path", message = "Path is not substring of expected message")
        assertEquals(path, exception.path, message = "Path is not equal to expected path")
        hasPath = true
    }

    fun offset(offset: Int) {
        if (mode == JsonTestingMode.TREE && exception.offset == -1) return
        assertContains(
            exception.message,
            "Unexpected JSON token at offset $offset: ",
            message = "Offset is not substring of expected message"
        )
        assertEquals(offset, exception.offset, message = "Offset is not equal to expected offset")
        hasOffset = true
    }

    fun hint(hint: String) {
        assertContains(exception.message, hint, message = "Hint is not substring of expected message")
        assertContains(exception.hint!!, hint, message = "Hint is not substring of expected hint")
        hasHint = true
    }

    fun input(input: String) {
        if (mode == JsonTestingMode.TREE) return // Inputs generally show only current sub-object for TreeDecoder
        assertContains(
            exception.message,
            "JSON input: ",
            message = "Input pragma is not substring of expected message"
        )
        assertContains(
            exception.message,
            input,
            message = "Input content is not substring of expected message"
        )
        assertEquals(input, exception.input, message = "Input is not equal to expected input")
    }

    fun noInput() {
        assertNull(exception.input, "Input is not null")
    }

    fun assertMissing() {
        if (!hasPath) assertNull(exception.path, "Path is not null")
        if (!hasOffset) assertEquals(-1, exception.offset, "Offset is not -1")
        if (!hasHint) assertNull(exception.hint, "Hint is not null")
    }

}

inline fun checkDecodingException(
    mode: JsonTestingMode,
    action: () -> Any?,
    assertions: DecodingExceptionAsserter.() -> Unit
) {
    val e = assertFailsWith(JsonDecodingException::class, action)
    val asserter = DecodingExceptionAsserter(mode, e)
    val result= runCatching {
        asserter.assertions()
        asserter.assertMissing()
    }
    if (result.isFailure) {
        println("Test failed for exception: ${asserter.exception.message}")
        asserter.exception.printStackTrace() // cannot do initCause outside of JVM
        throw result.exceptionOrNull()!!
    }
}
