/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class TuplesTest : JsonTestBase() {
    @Serializable
    data class MyPair<K, V>(val k: K, val v: V)

    @Serializable
    data class PairWrapper(val p: Pair<Int, String>)

    @Serializable
    data class TripleWrapper(val t: Triple<Int, String, Boolean>)

    @Test
    fun testCustomPair() = assertStringFormAndRestored(
        """{"k":42,"v":"foo"}""",
        MyPair(42, "foo"),
        MyPair.serializer(
            Int.serializer(),
            String.serializer()
        ),
        lenient
    )

    @Test
    fun testStandardPair() = assertStringFormAndRestored(
        """{"p":{"first":42,"second":"foo"}}""",
        PairWrapper(42 to "foo"),
        PairWrapper.serializer(),
        lenient
    )

    @Test
    fun testStandardPairHasCorrectDescriptor() {
        val desc = PairWrapper.serializer().descriptor.getElementDescriptor(0)
        assertEquals(desc.serialName, "kotlin.Pair")
        assertEquals(
            desc.elementDescriptors.map(SerialDescriptor::kind),
            listOf(PrimitiveKind.INT, PrimitiveKind.STRING)
        )
    }

    @Test
    fun testStandardTriple() = assertStringFormAndRestored(
        """{"t":{"first":42,"second":"foo","third":false}}""",
        TripleWrapper(Triple(42, "foo", false)),
        TripleWrapper.serializer(),
        lenient
    )

    @Test
    fun testStandardTripleHasCorrectDescriptor() {
        val desc = TripleWrapper.serializer().descriptor.getElementDescriptor(0)
        assertEquals(desc.serialName, "kotlin.Triple")
        assertEquals(
            desc.elementDescriptors.map(SerialDescriptor::kind),
            listOf(PrimitiveKind.INT, PrimitiveKind.STRING, PrimitiveKind.BOOLEAN)
        )
    }
}
