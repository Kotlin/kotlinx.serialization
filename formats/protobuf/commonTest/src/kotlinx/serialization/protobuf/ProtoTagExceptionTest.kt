/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtoTagExceptionTest {

    @Serializable
    data class TestDataToBuildWrongWireType(
        @ProtoNumber(1) val a: Int,
        @ProtoNumber(2) val b: Int,
    )

    @Serializable
    data class TestData(
        @ProtoNumber(1) val a: Int,
        @ProtoNumber(2) val b: String,
    )

    @Test
    fun testWrongTypeMessage() {
        val build = ProtoBuf.encodeToHexString(TestDataToBuildWrongWireType(42, 42))

        assertFailsWith<IllegalArgumentException>(
            assertion = {
                assertFailsWith(
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Error while decoding proto number 2 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Expected wire type SIZE_DELIMITED(2), but found VARINT(0)",
                )
            }
        ) {
            ProtoBuf.decodeFromHexString<TestData>(build)
        }
    }

    @Serializable
    data class TestNestedDataToBuild(
        @ProtoNumber(1) val nested: TestDataToBuildWrongWireType,
        @ProtoNumber(2) val a: String,
    )

    @Serializable
    data class TestNestedData(
        @ProtoNumber(1) val nested: TestData,
        @ProtoNumber(2) val a: String,
    )

    @Test
    fun `require string in nested message but got int`() {
        val build = ProtoBuf.encodeToHexString(TestNestedDataToBuild(TestDataToBuildWrongWireType(42, 42), "foo"))

        assertFailsWith<IllegalArgumentException>(
            assertion = {
                assertFailsWith(
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestNestedData",
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData at proto number 1 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestNestedData",
                    "Error while decoding proto number 2 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Expected wire type SIZE_DELIMITED(2), but found VARINT(0)",
                )
            }
        ) {
            ProtoBuf.decodeFromHexString<TestNestedData>(build)
        }
        assertFailsWith<IllegalArgumentException>(
            assertion = {
                assertFailsWith(
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Error while decoding proto number 1 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Expected wire type VARINT(0), but found SIZE_DELIMITED(2)",
                )
            }
        ) {
            ProtoBuf.decodeFromHexString<TestData>(build)
        }
    }

    @Test
    fun `require int in top message but get nested message`() {
        val build = ProtoBuf.encodeToHexString(TestNestedDataToBuild(TestDataToBuildWrongWireType(42, 42), "foo"))
        assertFailsWith<IllegalArgumentException>(
            assertion = {
                assertFailsWith(
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Error while decoding proto number 1 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                    "Expected wire type VARINT(0), but found SIZE_DELIMITED(2)",
                )
            }
        ) {
            ProtoBuf.decodeFromHexString<TestData>(build)
        }
    }


    @Serializable
    data class DuplicatingIdData(
        @ProtoOneOf val bad: IDuplicatingIdType,
        @ProtoNumber(3) val d: Int,
    )

    @Serializable
    sealed interface IDuplicatingIdType

    @Serializable
    data class DuplicatingIdStringType(@ProtoNumber(3) val s: String) : IDuplicatingIdType

    @Test
    fun testDuplicatedIdClass() {
        val duplicated = DuplicatingIdData(DuplicatingIdStringType("foo"), 42)
        // Fine to encode duplicated proto number properties in wire data
        ProtoBuf.encodeToHexString(duplicated).also {
            /**
             * 3:LEN {"foo"}
             * 3:VARINT 42
             */
            assertEquals("1a03666f6f182a", it)
        }

        // Without checking duplication of proto numbers,
        // ProtoBuf just throw exception about wrong wire type
        assertFailsWith<IllegalArgumentException>(
//            "Duplicated proto number 3 in kotlinx.serialization.protobuf.ProtobufOneOfTest.DuplicatingIdData for elements: d, bad."
            assertion = {
                assertFailsWith(
                    "Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.DuplicatingIdData",
                    "Error while decoding proto number 3 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.DuplicatingIdData",
                    "Expected wire type VARINT(0), but found SIZE_DELIMITED(2)",
                )
            }
        ) {
            /**
             * 3:LEN {"foo"}
             * 3:VARINT 42
             */
            ProtoBuf.decodeFromHexString<DuplicatingIdData>("1a03666f6f182a")
        }
    }
}