/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

// TODO these tests are far from being comoplete
class ProtobufPrimitivesTest {

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
        testConversion(TestList(listOf(150, 228, 1337)), TestList.serializer(), "08960108E40108B90A")

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
}
