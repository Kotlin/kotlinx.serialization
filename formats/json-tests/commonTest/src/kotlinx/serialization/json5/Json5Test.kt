/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json5

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class Json5Test {

    private val conf = Json {
        isLenient = false
        allowTrailingComma = true
        allowSpecialFloatingPointValues = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }.configuration

    val json5 = Json5(conf, EmptySerializersModule())

    val input = """{
        unquoted: 'and you can quote me on that',
        singleQuotes: 'I can use "double quotes" here',
        unknown: 'unknown',
        leadingDecimalPoint: .8675309, andTrailing: 8675309.,
        positiveSign: +1,
        "backwardsCompatible": "with JSON",}
    """.trimIndent()

    @Serializable
    data class Sample(
        val unquoted: String,
        val singleQuotes: String,
        val backwardsCompatible: String,
        val positiveSign: Int,
        val leadingDecimalPoint: Double, val andTrailing: Double
    )

    @Test
    fun canParseDocumentationSample() {
        val sample = json5.decodeFromString<Sample>(input)
        assertEquals(
            Sample(
                "and you can quote me on that",
                "I can use \"double quotes\" here",
                "with JSON",
                1, 0.8675309, 8675309.0
            ), sample
        )
    }

    @Test
    fun canParseUnquotedUnicodeEscapes() {
        val inputS = """{"\uD83D\uDCA9":"\uD83D\uDCA9"}"""
        val inputS2 = """{\uD83D\uDCA9:"\uD83D\uDCA9"}"""
        val base = Json.parseToJsonElement(inputS)
        assertEquals(base, json5.parseToJsonElement(inputS2))
    }
}
