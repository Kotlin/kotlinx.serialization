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

    private fun <T> testConversion(data: T, serializer: KSerializer<T>, expectedHexString: String) {
        val string = ProtoBuf.dumps(serializer, data).toUpperCase()
        assertEquals(expectedHexString, string)
        assertEquals(data, ProtoBuf.loads(serializer, string))
    }

    @Test
    fun testSignedInteger() {
        testConversion(TestInt(-150), TestInt.serializer(), "08AB02")
    }

    @Test
    fun testIntList() {
        testConversion(TestList(listOf(1, 2, 3)), TestList.serializer(), "080108020803")
    }

    @Test
    fun testString() {
        testConversion(TestString("testing"), TestString.serializer(), "120774657374696E67")
    }

    @Test
    fun testTwiceNested() {
        testConversion(TestInner(TestInt(-150)), TestInner.serializer(), "1A0308AB02")
    }

    @Test
    fun testMixedTags() {
        testConversion(TestComplex(42, "testing"), TestComplex.serializer(), "D0022A120774657374696E67")
    }

    @Test
    fun testDefaultPrimitiveValues() {
        testConversion(TestInt(0), TestInt.serializer(), "0800")
        testConversion(TestList(listOf()), TestList.serializer(), "")
        testConversion(TestString(""), TestString.serializer(), "1200")
    }

    @Test
    fun testFixedIntWithLong() {
        testConversion(TestNumbers(100500, Long.MAX_VALUE), TestNumbers.serializer(), "0D9488010010FFFFFFFFFFFFFFFF7F")
    }


    @Serializable
    class Foo(val l: List<Int> = emptyList(), val i: Int? = null,  val nextPage: Boolean = false)

    @Test
    fun foo() {
        testConversion(TestList(listOf(1)), TestList.serializer(), "0801")

    }

}
