/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json5

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class Json5Test {

    val conf = Json {
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
    data class Heh(val unquoted: String, val singleQuotes: String, val backwardsCompatible: String, val positiveSign: Double)

    @Test
    fun test1() {
        val h = json5.decodeFromString<Heh>(input)
        println(h)
    }
}
