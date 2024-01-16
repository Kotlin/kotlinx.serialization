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
        positiveSign: +1,
        "backwardsCompatible": "with JSON",}
    """.trimIndent()

    @Serializable
    data class Heh(val unquoted: String, val singleQuotes: String, val backwardsCompatible: String, val positiveSign: Int)

    @Test
    fun test1() {
        val h = json5.decodeFromString<Heh>(input)
        assertEquals(
            Heh(
                "and you can quote me on that",
                "I can use \"double quotes\" here",
                "with JSON",
                1
            ), h
        )
        println(h)
    }

    @Test
    fun canParseUnquotedUnicodeEscapes() {
        val inputS = """{"\uD83D\uDCA9":"\uD83D\uDCA9"}"""
        val inputS2 = """{\uD83D\uDCA9:"\uD83D\uDCA9"}"""
        val base = Json.parseToJsonElement(inputS)
        assertEquals(base, json5.parseToJsonElement(inputS2))
    }
}
