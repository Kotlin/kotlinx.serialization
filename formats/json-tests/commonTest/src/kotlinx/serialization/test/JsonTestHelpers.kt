/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

inline fun <reified T> testConversion(json: Json, data: T, expectedString: String) {
    assertEquals(expectedString, json.encodeToString(data))
    assertEquals(data, json.decodeFromString(expectedString))

    jvmOnly {
        assertEquals(expectedString, json.encodeViaStream(serializer(), data))
        assertEquals(data, json.decodeViaStream(serializer(), expectedString))
    }

    val jsonElement = json.encodeToJsonElement(data)
    assertEquals(expectedString, jsonElement.toString())
    assertEquals(data, json.decodeFromJsonElement(jsonElement))
}

inline fun <reified T> testConversion(data: T, expectedString: String) =
    testConversion(Json, data, expectedString)
