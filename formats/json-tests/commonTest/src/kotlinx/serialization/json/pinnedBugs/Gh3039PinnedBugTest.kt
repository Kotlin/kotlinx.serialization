/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/3039
 * Pins the current behavior that surfaced in the specific issue.
 *
 * encodeToJsonElement() drops the class discriminator when serializing a bare value class
 * that wraps a non-primitive in a sealed interface, even though encodeToString() includes it.
 * The discriminator is only missing for the bare top-level value, and has it when nested
 * inside another class, or as a list element.
 */
class Gh3039PinnedBugTest {

    @Serializable
    sealed interface A

    @Serializable
    data class B(val foo: String)

    @JvmInline
    @Serializable
    @SerialName("C")
    value class C(val b: B) : A

    @Serializable
    data class D(val bar: A)

    @Test
    fun test() {
        val bare: A = C(B("1"))
        assertEquals("""{"type":"C","foo":"1"}""", Json.encodeToString(bare))
        // Bug: the discriminator is missing here.
        assertEquals("""{"foo":"1"}""", Json.encodeToJsonElement(bare).toString())
        // Works as expected when nested inside another class, the discriminator is always present.
        val nested = D(C(B("2")))
        assertEquals("""{"bar":{"type":"C","foo":"2"}}""", Json.encodeToString(nested))
        assertEquals("""{"bar":{"type":"C","foo":"2"}}""", Json.encodeToJsonElement(nested).toString())
        // Works as expected when used as a list element, the discriminator is always present.
        val list: List<A> = listOf(C(B("3")))
        assertEquals("""[{"type":"C","foo":"3"}]""", Json.encodeToString(list))
        assertEquals("""[{"type":"C","foo":"3"}]""", Json.encodeToJsonElement(list).toString())
    }
}
