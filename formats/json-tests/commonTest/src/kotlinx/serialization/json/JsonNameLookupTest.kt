/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonNameLookupTest : JsonTestBase() {

    @Serializable
    data class HashCollision(
        @SerialName("Aa") val first: Int,
        @SerialName("BB") val second: Int
    )

    @Serializable
    data class EscapedName(@SerialName("quoted\"name") val value: Int)

    @Serializable
    data class DifferentLengths(val longerName: Int, val x: Int)

    @Serializable
    data class Recursive(val head: Int, val child: Recursive? = null, val tail: Int)

    @Test
    fun orderedAndOutOfOrderNames() {
        assertEquals(
            HashCollision(1, 2),
            Json.decodeFromString<HashCollision>("""{"Aa":1,"BB":2}""")
        )
        assertEquals(
            HashCollision(1, 2),
            Json.decodeFromString<HashCollision>("""{"BB":2,"Aa":1}""")
        )
        assertEquals(
            DifferentLengths(1, 2),
            Json.decodeFromString<DifferentLengths>("""{"x":2,"longerName":1}""")
        )
    }

    @Test
    fun escapedNameUsesFallback() {
        assertEquals(
            EscapedName(42),
            Json.decodeFromString<EscapedName>("""{"quoted\"name":42}""")
        )
    }

    @Test
    fun nestedSameDescriptorRestoresThroughFallback() {
        assertEquals(
            Recursive(1, Recursive(2, tail = 3), 4),
            Json.decodeFromString<Recursive>("""{"head":1,"child":{"head":2,"tail":3},"tail":4}""")
        )
    }
}
