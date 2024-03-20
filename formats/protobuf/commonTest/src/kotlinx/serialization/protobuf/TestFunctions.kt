/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
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
