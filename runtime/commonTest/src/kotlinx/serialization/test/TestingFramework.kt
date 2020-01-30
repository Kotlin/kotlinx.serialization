/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*


inline fun <reified T : Any> assertStringForm(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = Json.plain,
    printResult: Boolean = false
) {
    val string = format.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
}


inline fun <reified T : Any> assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = Json.plain,
    printResult: Boolean = false
) {
    val string = format.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = format.parse(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

inline fun <reified T : Any> StringFormat.assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    printResult: Boolean = false
) {
    val string = this.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = this.parse(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)

inline fun <reified T : Throwable> assertFailsWithMessage(
    message: String,
    assertionMessage: String? = null,
    block: () -> Unit
) {
    val exception = assertFailsWith(T::class, assertionMessage, block)
    assertTrue(
        exception.message!!.contains(message),
        "Expected message '${exception.message}' to contain substring '$message'"
    )
}
