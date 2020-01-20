/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.test.*

inline fun <reified T : Any> assertSerializedToBinaryAndRestored(
    original: T,
    serializer: KSerializer<T>,
    format: BinaryFormat,
    printResult: Boolean = false,
    hexResultToCheck: String? = null
) {
    val bytes = format.dump(serializer, original)
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
    val restored = format.load(serializer, bytes)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}
