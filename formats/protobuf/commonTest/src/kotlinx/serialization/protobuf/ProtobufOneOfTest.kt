/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.test.*

class ProtobufOneOfTest {
    @Serializable
    data class OneOfData(
        @ProtoOneOf val i: IType,
        @ProtoNumber(3) val name: String
    )

    @Serializable
    data class OneOfDataNullable(
        @ProtoOneOf val i: IType?,
        @ProtoNumber(3) val name: String
    )

    @Serializable
    data class DataNoOneOf(
        @ProtoNumber(5) val i: Int = 0,
        @ProtoNumber(6) val s: String = "",
        @ProtoNumber(3) val name: String,
    )

    @Serializable
    sealed interface IType

    @Serializable
    data class IntType(@ProtoNumber(5) val i: Int = 0) : IType

    @Serializable
    @JvmInline
    value class StringType(@ProtoNumber(6) val s: String) : IType

    @Serializable
    data class NestedIntType(@ProtoNumber(7) val intType: IntType) : IType

    @Test
    fun testOneOfIntType() {
        val dataInt = OneOfData(IntType(42), "foo")
        val intString = ProtoBuf.encodeToHexString(OneOfData.serializer(), dataInt).also { println(it) }
        /**
         * 5: 42
         * 3: {"foo"}
         */
        assertEquals("282a1a03666f6f", intString)
    }

    @Test
    fun testOneOfStringType() {
        val dataString = OneOfData(StringType("bar"), "foo")
        val stringString = ProtoBuf.encodeToHexString(OneOfData.serializer(), dataString).also { println(it) }
        /**
         * 6: {"bar"}
         * 3: {"foo"}
         */
        assertEquals("32036261721a03666f6f", stringString)
    }

    @Test
    fun testOneOfDecodeStringType() {
        val dataString = ProtoBuf.decodeFromHexString<OneOfData>("32036261721a03666f6f")
        assertEquals(OneOfData(StringType("bar"), "foo"), dataString)
    }

    @Test
    fun testOneOfDecodeIntType() {
        val dataInt = ProtoBuf.decodeFromHexString<OneOfData>("282a1a03666f6f")
        assertEquals(OneOfData(IntType(42), "foo"), dataInt)
    }

    @Test
    fun testOneOfIntTypeNullable() {
        val dataInt = OneOfDataNullable(IntType(42), "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataInt).also {
            println(it)
            assertEquals("282a1a03666f6f", it)
        }

    }

    @Test
    fun testOneOfStringTypeNullable() {
        val dataString = OneOfDataNullable(StringType("bar"), "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataString).also {
            println(it)
            assertEquals("32036261721a03666f6f", it)
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
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("32036261721a03666f6f").let {
            assertEquals(OneOfDataNullable(StringType("bar"), "foo"), it)
        }
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("282a1a03666f6f").let {
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
    data class FailType(@ProtoNumber(8) val i: Int, @ProtoNumber(9) val j: Int) : IType

    @Test
    fun testOneOfElementCheck() {
        val data = OneOfData(FailType(1, 2), "foo")
        assertFailsWithMessage<IllegalArgumentException>(
            message = "Implementation of oneOf type" +
                " kotlinx.serialization.protobuf.ProtobufOneOfTest.FailType" +
                " should contain only 1 element, but get 2"
        ) {
            ProtoBuf.encodeToHexString(OneOfData.serializer(), data)
        }
    }

    @Serializable
    data class NestedOneOfType(@ProtoNumber(9) val i: InnerNested) : IType

    @Serializable
    data class InnerNested(@ProtoOneOf val i: InnerOneOf)

    @Serializable
    sealed interface InnerOneOf

    @Serializable
    data class InnerInt(@ProtoNumber(1) val i: Int) : InnerOneOf

    @Serializable
    data class InnerString(@ProtoNumber(2) val s: String) : InnerOneOf

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
        @ProtoOneOf val one: IType,
        @ProtoNumber(3) val name: String,
        @ProtoOneOf val two: OtherType
    )

    interface OtherType

    @Serializable
    data class OtherIntType(@ProtoNumber(11) val i: Int) : OtherType

    @Serializable
    data class OtherStringType(@ProtoNumber(12) val s: String) : OtherType

    @Test
    fun testEncodeDoubleOneOf() {
        val module = SerializersModule {
            polymorphic(OtherType::class) {
                subclass(OtherStringType::class)
            }
        }
        val buf = ProtoBuf {
            serializersModule = module
        }
        val data = DoubleOneOfElement(
            IntType(32),
            "foo",
            OtherStringType("bar")
        )
        buf.encodeToHexString(DoubleOneOfElement.serializer(), data).also {
            println(it)
            assertEquals("28201a03666f6f6203626172", it)
        }

        assertFailsWithMessage<SerializationException>(
            message = "Serializer for subclass 'OtherIntType' is not found in the polymorphic scope of 'OtherType'."
        ) {
            buf.encodeToHexString(
                DoubleOneOfElement.serializer(), DoubleOneOfElement(
                    IntType(32),
                    "foo",
                    OtherIntType(32)
                )
            )
        }
    }

    @Test
    fun testDecodeDoubleOneOf() {
        val module = SerializersModule {
            polymorphic(OtherType::class) {
                subclass(OtherStringType::class)
            }
        }
        val buf = ProtoBuf {
            serializersModule = module
        }
        val data = DoubleOneOfElement(
            IntType(32),
            "foo",
            OtherStringType("bar")
        )
        buf.decodeFromHexString<DoubleOneOfElement>("28201a03666f6f6203626172").also {
            assertEquals(data, it)
        }
    }

    @Test
    fun testCustomerModule() {
        val module = SerializersModule {
            polymorphic(IType::class) {
                subclass(IntType::class, IntType.serializer())
                subclass(StringType::class, StringType.serializer())
            }
        }

        val buf = ProtoBuf { serializersModule = module }

        val dataInt = OneOfData(IntType(42), "foo")
        val intString = buf.encodeToHexString(OneOfData.serializer(), dataInt).also { println(it) }
        /**
         * 5: 42
         * 3: {"foo"}
         */
        assertEquals("282a1a03666f6f", intString)

        val dataString = OneOfData(StringType("bar"), "foo")
        val stringString = buf.encodeToHexString(OneOfData.serializer(), dataString).also { println(it) }
        /**
         * 6: {"bar"}
         * 3: {"foo"}
         */
        assertEquals("32036261721a03666f6f", stringString)
        val stringData = buf.decodeFromHexString<OneOfData>(stringString)
        assertEquals(stringData, dataString)
        val intData = buf.decodeFromHexString<OneOfData>(intString)
        assertEquals(intData, dataInt)
    }

    @Serializable
    data class FailWithClass(@ProtoOneOf val i: IFailType, @ProtoNumber(3) val name: String)

    @Serializable
    sealed interface IFailType

    @Serializable
    data class FailIntType(val i: Int) : IFailType

    @Test
    fun testFailWithClassEncoding() {
        val data = FailWithClass(FailIntType(42), "foo")
        assertFailsWith<IllegalArgumentException> { ProtoBuf.encodeToHexString(FailWithClass.serializer(), data) }
    }

    @Test
    fun testFailWithClassDecoding() {
        assertFailsWith<MissingFieldException> {
            ProtoBuf.decodeFromHexString<FailWithClass>(
                "282a1a03666f6f"
            )
        }
    }

    @Serializable
    data class CustomOuter(@ProtoOneOf val inner: CustomInner)

    @Serializable
    abstract class CustomInner

    data class CustomInnerInt(val i: Int) : CustomInner()

    object CustomerInnerIntSerializer : KSerializer<CustomInnerInt> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("CustomInnerInt") {
                element<Int>(
                    "i",
                    annotations = listOf(ProtoNumber(1)),
                )
            }

        override fun deserialize(decoder: Decoder): CustomInnerInt {
            return decoder.decodeStructure(descriptor) {
                var value = 0
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> value = decodeIntElement(descriptor, index)
                        CompositeDecoder.DECODE_DONE -> break
                    }
                }
                CustomInnerInt(value)
            }
        }

        override fun serialize(encoder: Encoder, value: CustomInnerInt) = encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.i)
        }

    }

    @Test
    fun testCustom() {
        val module = SerializersModule {
            polymorphic(CustomInner::class) {
                subclass(CustomInnerInt::class, CustomerInnerIntSerializer)
            }
        }
        val data = CustomOuter(CustomInnerInt(42))
        val buf = ProtoBuf { serializersModule = module }
        assertEquals("082a", buf.encodeToHexString(CustomOuter.serializer(), data))
    }

    @Serializable
    data class CustomAnyData(@ProtoOneOf @Polymorphic val inner: Any)

    @Test
    fun testCustomAny() {
        val module = SerializersModule {
            polymorphic(Any::class) {
                subclass(CustomInnerInt::class, CustomerInnerIntSerializer)
            }
        }
        val data = CustomAnyData(CustomInnerInt(42))
        val buf = ProtoBuf { serializersModule = module }
//        assertEquals("082a", buf.encodeToHexString(data))
        assertEquals(data, buf.decodeFromHexString<CustomAnyData>("082a"))
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
        ProtoBuf.encodeToHexString(duplicated).also {
            /**
             * 3: {"foo"}
             * 3: 42
             */
            assertEquals("1a03666f6f182a", it)
        }

        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.decodeFromHexString<DuplicatingIdData>("1a03666f6f182a")
        }
    }

    @Serializable
    data class TypedIntOuter(
        @ProtoOneOf val i: ITypedInt,
    )

    @Serializable
    sealed interface ITypedInt

    @Serializable
    data class Fixed32Int(
        @ProtoNumber(2)
        @ProtoType(ProtoIntegerType.FIXED)
        val value: Int
    ): ITypedInt

    @Serializable
    @JvmInline
    value class Fixed32Long(
        @ProtoNumber(3)
        @ProtoType(ProtoIntegerType.FIXED)
        val value: Long
    ): ITypedInt

    @Serializable
    @JvmInline
    value class SignedInt(
        @ProtoNumber(4)
        @ProtoType(ProtoIntegerType.SIGNED)
        val value: Int
    ): ITypedInt

    @Serializable
    data class SignedLong(
        @ProtoNumber(5)
        @ProtoType(ProtoIntegerType.SIGNED)
        val value: Long
    ): ITypedInt

    @Serializable
    data class DefaultInt(
        @ProtoNumber(6)
        @ProtoType(ProtoIntegerType.DEFAULT)
        val value: Int
    ): ITypedInt
    @Serializable
    data class DefaultLong(
        @ProtoNumber(7)
        @ProtoType(ProtoIntegerType.DEFAULT)
        val value: Long
    ): ITypedInt

    @Test
    fun testTypedInt() {
        val fixed = TypedIntOuter(Fixed32Int(32))
        ProtoBuf.encodeToHexString(fixed).also {
            println(it)
            // 2: 32i32
            assertEquals("1520000000", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("1520000000").also {
            assertEquals(fixed, it)
        }
        val fixedLong = TypedIntOuter(Fixed32Long(30576774159))
        ProtoBuf.encodeToHexString(fixedLong).also {
            println(it)
            // 3: 30576774159
            assertEquals("188f9892f471", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("188f9892f471").also {
            assertEquals(fixedLong, it)
        }
        val signed = TypedIntOuter(SignedInt(32))
        ProtoBuf.encodeToHexString(signed).also {
            println(it)
            // 4: 32
            assertEquals("2020", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("2020").also {
            assertEquals(signed, it)
        }
        val signedLong = TypedIntOuter(SignedLong(30576774159))
        ProtoBuf.encodeToHexString(signedLong).also {
            println(it)
            // 5: 61153548318 As sint: 30576774159
            assertEquals("289eb0a4e8e301", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("289eb0a4e8e301").also {
            assertEquals(signedLong, it)
        }
        val default = TypedIntOuter(DefaultInt(32))
        ProtoBuf.encodeToHexString(default).also {
            println(it)
            // 6: 32
            assertEquals("3020", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("3020").also {
            assertEquals(default, it)
        }
        val defaultLong = TypedIntOuter(DefaultLong(30576774159))
        ProtoBuf.encodeToHexString(defaultLong).also {
            println(it)
            // 7: 30576774159
            assertEquals("388f9892f471", it)
        }
        ProtoBuf.decodeFromHexString<TypedIntOuter>("388f9892f471").also {
            assertEquals(defaultLong, it)
        }
    }

}