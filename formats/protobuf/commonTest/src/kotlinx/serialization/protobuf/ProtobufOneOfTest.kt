/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufOneOfTest {
    @Serializable
    data class OneOfData(
        @ProtoOneOf(1, 2, 7, 8, 9) val i: IType,
        @ProtoNumber(3) val name: String
    )

    @Serializable
    data class OneOfDataNullable(
        @ProtoOneOf(1, 2, 5) val i: IType?,
        @ProtoNumber(3) val name: String
    )

    @Serializable
    data class DataNoOneOf(
        val i: Int = 0,
        val s: String = "",
        val name: String
    )

    @Serializable
    sealed interface IType

    @Serializable
    @ProtoNumber(1)
    data class IntType(@ProtoNumber(5) val i: Int = 0) : IType

    @Serializable
    @ProtoNumber(2)
    data class StringType(@ProtoNumber(6) val s: String) : IType

    @Serializable
    @ProtoNumber(7)
    data class NestedIntType(val intType: IntType) : IType

    @Test
    fun testOneOfIntType() {
        val dataInt = OneOfData(IntType(42), "foo")
        val intString = ProtoBuf.encodeToHexString(OneOfData.serializer(), dataInt).also { println(it) }
        /**
         * 1: 42
         * 3: {"foo"}
         */
        assertEquals("082a1a03666f6f", intString)
    }

    @Test
    fun testOneOfStringType() {
        val dataString = OneOfData(StringType("bar"), "foo")
        val stringString = ProtoBuf.encodeToHexString(OneOfData.serializer(), dataString).also { println(it) }
        /**
         * 2: {"bar"}
         * 3: {"foo"}
         */
        assertEquals("12036261721a03666f6f", stringString)
    }

    @Test
    fun testOneOfDecode() {
        val dataString = ProtoBuf.decodeFromHexString<OneOfData>("12036261721a03666f6f")
        assertEquals(OneOfData(StringType("bar"), "foo"), dataString)
        val dataInt = ProtoBuf.decodeFromHexString<OneOfData>("082a1a03666f6f")
        assertEquals(OneOfData(IntType(42), "foo"), dataInt)
    }

    @Test
    fun testOneOfIntTypeNullable() {
        val dataInt = OneOfDataNullable(IntType(42), "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataInt).also {
            println(it)
            assertEquals("082a1a03666f6f", it)
        }

    }

    @Test
    fun testOneOfStringTypeNullable() {
        val dataString = OneOfDataNullable(StringType("bar"), "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataString).also {
            println(it)
            assertEquals("12036261721a03666f6f", it)
        }
        val dataStringNull = OneOfDataNullable(null, "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataStringNull).also {
            println(it)
            /**
             * 3: {"foo"}
             */
            assertEquals("1a03666f6f", it)
        }
    }

    @Test
    fun testOneOfDecodeNullable() {
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("12036261721a03666f6f").let {
            assertEquals(OneOfDataNullable(StringType("bar"), "foo"), it)
        }
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("082a1a03666f6f").let {
            assertEquals(OneOfDataNullable(IntType(42), "foo"), it)
        }
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("1a03666f6f").let {
            assertEquals(OneOfDataNullable(null, "foo"), it)
        }
    }

    @Test
    fun testOneOfToFlatInt() {
        val dataInt = OneOfData(IntType(42), "foo")
        ProtoBuf.encodeToByteArray(dataInt).let {
            ProtoBuf.decodeFromByteArray<DataNoOneOf>(it).let { data ->
                assertEquals(
                    DataNoOneOf(42, "", "foo"), data
                )
            }
        }
    }

    @Test
    fun testOneOfToFlatString() {
        val dataString = OneOfData(StringType("bar"), "foo")
        ProtoBuf.encodeToByteArray(dataString).let {
            ProtoBuf.decodeFromByteArray<DataNoOneOf>(it).let { data ->
                assertEquals(
                    DataNoOneOf(0, "bar", "foo"), data
                )
            }
        }
    }

    @Test
    fun testFlatIntToOneOf() {
        val dataInt = DataNoOneOf(42, "", "foo")
        ProtoBuf.encodeToByteArray(dataInt).let {
            ProtoBuf.decodeFromByteArray<OneOfData>(it).let { data ->
                assertEquals(
                    OneOfData(IntType(42), "foo"), data
                )
            }
        }
    }

    @Test
    fun testFlatStringToOneOf() {
        val dataString = DataNoOneOf(0, "bar", "foo")
        ProtoBuf.encodeToByteArray(dataString).let {
            ProtoBuf.decodeFromByteArray<OneOfData>(it).let { data ->
                assertEquals(
                    OneOfData(StringType("bar"), "foo"), data
                )
            }
        }
    }

    @Test
    fun testEncodeNestedStruct() {
        val data = OneOfData(NestedIntType(IntType(32)), "foo")
        ProtoBuf.encodeToHexString(OneOfData.serializer(), data).also {
            println(it)
            /**
             * 7: {5: 32}
             * 3: {"foo"}
             */
            assertEquals("3a0228201a03666f6f", it)
        }
    }

    @Test
    fun testDecodeNestedStruct() {
        val data = OneOfData(NestedIntType(IntType(32)), "foo")
        ProtoBuf.decodeFromHexString<OneOfData>("3a0228201a03666f6f").also {
            println(it)
            assertEquals(data, it)
        }
    }

    @Serializable
    @ProtoNumber(8)
    data class FailType(val i: Int, val j: Int) : IType

    @Test
    fun testOneOfElementCheck() {
        val data = OneOfData(FailType(1, 2), "foo")
        assertFailsWith<IllegalArgumentException> { ProtoBuf.encodeToHexString(OneOfData.serializer(), data) }
    }

    @Serializable
    @ProtoNumber(9)
    data class NestedOneOfType(val i: InnerNested) : IType

    @Serializable
    data class InnerNested(@ProtoOneOf(1, 2) val i: InnerOneOf)

    @Serializable
    sealed interface InnerOneOf

    @Serializable
    @ProtoNumber(1)
    data class InnerInt(val i: Int) : InnerOneOf

    @Serializable
    @ProtoNumber(2)
    data class InnerString(val s: String) : InnerOneOf

    @Test
    fun testEncodeNestedOneOf() {
        val data = OneOfData(NestedOneOfType(i = InnerNested(InnerInt(32))), "foo")
        ProtoBuf.encodeToHexString(OneOfData.serializer(), data).also {
            println(it)
            /**
             * 9: {1: 32}
             * 3: {"foo"}
             */
            assertEquals("4a0208201a03666f6f", it)
        }
    }

    @Test
    fun testDecodeNestedOneOf() {
        val data = OneOfData(NestedOneOfType(i = InnerNested(InnerInt(32))), "foo")
        ProtoBuf.decodeFromHexString<OneOfData>("4a0208201a03666f6f").also {
            println(it)
            /**
             * 9: {1: 32}
             * 3: {"foo"}
             */
            assertEquals(data, it)
        }
    }

    @Serializable
    data class DoubleOneOfElement(
        @ProtoOneOf(1, 2) val one: IType,
        @ProtoNumber(3) val name: String,
        @ProtoOneOf(4, 5) val two: OtherType
    )

    @Serializable sealed interface OtherType

    @Serializable @ProtoNumber(4) data class OtherIntType(val i: Int): OtherType

    @Serializable @ProtoNumber(5) data class OtherStringType(val s: String): OtherType

    @Test
    fun testEncodeDoubleOneOf() {
        val data = DoubleOneOfElement(
            IntType(32),
            "foo",
            OtherStringType("bar")
        )
        ProtoBuf.encodeToHexString(DoubleOneOfElement.serializer(), data).also {
            println(it)
            assertEquals("08201a03666f6f2a03626172", it)
        }
    }

    @Test
    fun testDecodeDoubleOneOf() {
        val data = DoubleOneOfElement(
            IntType(32),
            "foo",
            OtherStringType("bar")
        )
        ProtoBuf.decodeFromHexString<DoubleOneOfElement>("08201a03666f6f2a03626172").also {
            assertEquals(data, it)
        }
    }
}