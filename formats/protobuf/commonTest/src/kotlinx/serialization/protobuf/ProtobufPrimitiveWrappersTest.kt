/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

/*
 * TODO improve these tests:
 *  * All primitive types
 *  * All nullable types
 *  * Built-in types (TBD)
 *  * Primitives nullability
 */
class ProtobufPrimitiveWrappersTest {

    @Test
    fun testSignedInteger() {
        assertSerializedToBinaryAndRestored(TestInt(-150), TestInt.serializer(), ProtoBuf, hexResultToCheck = "08AB02")
    }

    @Test
    fun testIntList() {
        assertSerializedToBinaryAndRestored(
            TestList(listOf(1, 2, 3)),
            TestList.serializer(), ProtoBuf, hexResultToCheck = "080108020803"
        )
    }

    @Test
    fun testString() {
        assertSerializedToBinaryAndRestored(
            TestString("testing"),
            TestString.serializer(), ProtoBuf, hexResultToCheck = "120774657374696E67"
        )
    }

    @Test
    fun testTwiceNested() {
        assertSerializedToBinaryAndRestored(
            TestInner(TestInt(-150)),
            TestInner.serializer(), ProtoBuf, hexResultToCheck = "1A0308AB02"
        )
    }

    @Test
    fun testMixedTags() {
        assertSerializedToBinaryAndRestored(
            TestComplex(42, "testing"),
            TestComplex.serializer(), ProtoBuf, hexResultToCheck = "D0022A120774657374696E67"
        )
    }

    @Test
    fun testDefaultPrimitiveValues() {
        assertSerializedToBinaryAndRestored(TestInt(0), TestInt.serializer(), ProtoBuf, hexResultToCheck = "0800")
        assertSerializedToBinaryAndRestored(TestList(listOf()), TestList.serializer(), ProtoBuf, hexResultToCheck = "")
        assertSerializedToBinaryAndRestored(
            TestString(""),
            TestString.serializer(), ProtoBuf, hexResultToCheck = "1200"
        )
    }

    @Test
    fun testFixedIntWithLong() {
        assertSerializedToBinaryAndRestored(
            TestNumbers(100500, Long.MAX_VALUE),
            TestNumbers.serializer(), ProtoBuf, hexResultToCheck = "0D9488010010FFFFFFFFFFFFFFFF7F"
        )
    }
}
