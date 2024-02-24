/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

inline fun <reified T> testConversion(json: Json, data: T, expectedHexString: String) {
    val string = json.encodeToString(data)
    assertEquals(expectedHexString, string)
    assertEquals(data, json.decodeFromString(string))
}

inline fun <reified T> testConversion(data: T, expectedHexString: String) =
    testConversion(Json, data, expectedHexString)
