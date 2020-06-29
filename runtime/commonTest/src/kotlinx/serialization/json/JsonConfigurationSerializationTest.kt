/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.Test
import kotlin.test.assertEquals

internal class JsonConfigurationSerializationTest: JsonTestBase() {
    private val json = default
    @Test
    fun fromJson() {
        val defaultString = """{"encodeDefaults":true,"ignoreUnknownKeys":false,"isLenient":false,"serializeSpecialFloatingPointValues":false,"allowStructuredMapKeys":false,"prettyPrint":false,"unquotedPrint":false,"indent":"    ","useArrayPolymorphism":false,"classDiscriminator":"type","updateMode":"OVERWRITE"}"""
        val defaultConfig = json.parse(JsonConfiguration.serializer(), defaultString)
        assertEquals(defaultConfig, JsonConfiguration.Default)

        val stableString = """{"encodeDefaults":true,"ignoreUnknownKeys":false,"isLenient":false,"serializeSpecialFloatingPointValues":false,"allowStructuredMapKeys":true,"prettyPrint":false,"unquotedPrint":false,"indent":"    ","useArrayPolymorphism":false,"classDiscriminator":"type","updateMode":"OVERWRITE"}"""
        val stableConfig = json.parse(JsonConfiguration.serializer(), stableString)
        assertEquals(stableConfig, JsonConfiguration.Stable)
    }

    @Test
    fun toJson() {
        val defaultString = json.stringify(JsonConfiguration.serializer(), JsonConfiguration.Default)
        val expectedDefault = """{"encodeDefaults":true,"ignoreUnknownKeys":false,"isLenient":false,"serializeSpecialFloatingPointValues":false,"allowStructuredMapKeys":false,"prettyPrint":false,"unquotedPrint":false,"indent":"    ","useArrayPolymorphism":false,"classDiscriminator":"type","updateMode":"OVERWRITE"}"""
        assertEquals(expectedDefault, defaultString)
        val stableString = json.stringify(JsonConfiguration.serializer(), JsonConfiguration.Stable)
        val expectedStable = """{"encodeDefaults":true,"ignoreUnknownKeys":false,"isLenient":false,"serializeSpecialFloatingPointValues":false,"allowStructuredMapKeys":true,"prettyPrint":false,"unquotedPrint":false,"indent":"    ","useArrayPolymorphism":false,"classDiscriminator":"type","updateMode":"OVERWRITE"}"""
        assertEquals(expectedStable, stableString)
    }
}