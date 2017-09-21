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

import org.junit.Test

class ProtobufReaderTest {

    @Test
    fun readSimpleObject() {
        ProtoBuf.loads<TestInt>("08ab02") shouldBe t1
    }

    @Test
    fun readObjectWithString() {
        ProtoBuf.loads<TestString>("120774657374696E67") shouldBe t3
    }

    @Test
    fun readObjectWithList() {
        ProtoBuf.loads<TestList>("08960108E40108B90A") shouldBe t2
    }

    @Test
    fun readInnerObject() {
        ProtoBuf.loads<TestInner>("1a0308ab02") shouldBe t4
    }

    @Test
    fun readObjectWithUnorderedTags() {
        ProtoBuf.loads<TestComplex>("120774657374696E67D0022A") shouldBe t5
    }

    @Test
    fun readObjectsWithEmptyValues() {
        ProtoBuf.loads<TestInt>("0800") shouldBe t1e
        ProtoBuf.loads<TestList>("") shouldBe t2e
        ProtoBuf.loads<TestString>("1200") shouldBe t3e
    }

    @Test
    fun readObjectWithUnknownFields() {
        ProtoBuf.loads<TestInt>("08960108E40108B90A08ab02120774657374696E67") shouldBe t1
    }

    @Test
    fun readNumbers() {
        ProtoBuf.loads<TestNumbers>("0d9488010010ffffffffffffffff7f") shouldBe t6
    }
}