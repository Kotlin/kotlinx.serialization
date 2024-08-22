/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.internal.ProtobufDecodingException
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
    fun testWrongIntFieldInNestedMessage() {
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
    fun testWrongStringFieldInNestedMessage() {
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
    data class TestDataWithMessageList(@ProtoNumber(1) @ProtoPacked val list: List<TestData>)

    @Serializable
    data class TestDataWithWrongList(@ProtoNumber(1) @ProtoPacked val list: List<TestDataToBuildWrongWireType>)

    @Test
    fun testWrongIntFieldInNestedMessageInList() {
        val build = ProtoBuf.encodeToHexString(TestDataWithWrongList(listOf(TestDataToBuildWrongWireType(42, 42))))
        assertFailsWith<ProtobufDecodingException>(
            assertion = {
                assertFailsWith("Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestDataWithMessageList")
                assertCausedBy<ProtobufDecodingException> {
                    assertFailsWith("Error while decoding kotlin.collections.ArrayList at proto number 1 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestDataWithMessageList")
                    assertCausedBy<ProtobufDecodingException> {
                        assertFailsWith(
                            "Error while decoding index 0 in repeated field of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                            "Error while decoding proto number 2 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                            "Expected wire type SIZE_DELIMITED(2), but found VARINT(0)",
                        )
                    }
                }
            }
        ) {
            val result = ProtoBuf.decodeFromHexString<TestDataWithMessageList>(build)
        }
    }

    @Serializable
    data class TestDataWithMessageMapValue(@ProtoNumber(1) val map: Map<String, TestData>)

    @Serializable
    data class TestDataWithWrongMapValue(@ProtoNumber(1) val map: Map<String, TestDataToBuildWrongWireType>)

    @Test
    fun testWrongIntFieldInNestedMapValue() {
        val build = ProtoBuf.encodeToHexString(TestDataWithWrongMapValue(map = mapOf("1" to TestDataToBuildWrongWireType(42, 42))))
        assertFailsWith<ProtobufDecodingException>(
            assertion = {
                assertFailsWith("Error while decoding kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestDataWithMessageMapValue")
                assertCausedBy<ProtobufDecodingException> {
                    assertFailsWith("Error while decoding kotlin.collections.LinkedHashMap at proto number 1 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestDataWithMessageMapValue")
                    assertCausedBy<ProtobufDecodingException> {
                        assertFailsWith(
                            "Error while decoding kotlin.collections.Map.Entry at proto number 1 of kotlin.collections.LinkedHashSet",
                            "Error while decoding value of index 0 in map field of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                            "Error while decoding proto number 2 of kotlinx.serialization.protobuf.ProtoTagExceptionTest.TestData",
                            "Expected wire type SIZE_DELIMITED(2), but found VARINT(0)",
                        )
                    }
                }
            }
        ) {
            ProtoBuf.decodeFromHexString<TestDataWithMessageMapValue>(build)
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

        assertFailsWith<IllegalArgumentException>(
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