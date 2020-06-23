/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.test.*

internal inline fun <reified T : Any> assertSerializedToBinaryAndRestored(
    original: T,
    serializer: KSerializer<T>,
    format: BinaryFormat,
    printResult: Boolean = false,
    hexResultToCheck: String? = null
) {
    val bytes = format.encodeToByteArray(serializer, original)
    val hexString = HexConverter.printHexBinary(bytes, lowerCase = true)
    if (printResult) {
        println("[Serialized form] $hexString")
    }
    if (hexResultToCheck != null) {
        assertEquals(
            hexResultToCheck.toLowerCase(),
            hexString,
            "Expected serialized binary to be equal in hex representation"
        )
    }
    val restored = format.decodeFromByteArray(serializer, bytes)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}
