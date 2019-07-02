/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.loads
import kotlinx.serialization.test.isNative
import kotlinx.serialization.test.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

class ProtobufReaderTest {

    @Test
    fun readSimpleObject() {
        ProtoBuf.loads(TestInt.serializer(), "08ab02") shouldBe t1
    }

    @Test
    fun readObjectWithString() {
        ProtoBuf.loads(TestString.serializer(), "120774657374696E67") shouldBe t3
    }

    @Test
    fun readObjectWithList() {
        ProtoBuf.loads(TestList.serializer(), "08960108E40108B90A") shouldBe t2
    }

    @Test
    fun readInnerObject() {
        ProtoBuf.loads(TestInner.serializer(), "1a0308ab02") shouldBe t4
    }

    @Test
    fun readObjectWithUnorderedTags() {
        ProtoBuf.loads(TestComplex.serializer(), "120774657374696E67D0022A") shouldBe t5
    }

    @Test
    fun readObjectsWithEmptyValues() {
        ProtoBuf.loads(TestInt.serializer(), "0800") shouldBe t1e
        ProtoBuf.loads(TestList.serializer(), "") shouldBe t2e
        ProtoBuf.loads(TestString.serializer(), "1200") shouldBe t3e
    }

    @Test
    fun readObjectWithUnknownFields() {
        ProtoBuf.loads(TestInt.serializer(), "08960108E40108B90A08ab02120774657374696E67") shouldBe t1
    }

    @Test
    fun readNumbers() {
        ProtoBuf.loads(TestNumbers.serializer(), "0d9488010010ffffffffffffffff7f") shouldBe t6
    }

    @Test
    fun mergeListIfSplitByAnotherField() {
        if (isNative()) return // todo: support update on Native
        ProtoBuf.loads(TestIntWithList.serializer(), "500308960150045005") shouldBe TestIntWithList(150, listOf(3, 4, 5))
    }
}
