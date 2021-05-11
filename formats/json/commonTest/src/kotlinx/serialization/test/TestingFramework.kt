/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*


inline fun <reified T : Any> assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = Json,
    printResult: Boolean = false
) {
    val string = format.encodeToString(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = format.decodeFromString(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

inline fun <reified T : Any> StringFormat.assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    printResult: Boolean = false
) {
    val string = this.encodeToString(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = this.decodeFromString(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

fun <T : Any> assertSerializedAndRestored(
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = Json,
    printResult: Boolean = false
) {
    if (printResult) println("[Input] $original")
    val string = format.encodeToString(serializer, original)
    if (printResult) println("[Serialized form] $string")
    val restored = format.decodeFromString(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

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
