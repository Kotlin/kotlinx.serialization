/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.test.*

class ReorderingCompositeEncoderTest {
    @Test
    fun shouldReorderWellWithDirectUsage() {
        val valueClassDescriptor = buildClassSerialDescriptor("inlined") { element<String>("value") }
        val descriptor = buildClassSerialDescriptor("reordered descriptor") {
            element<Double>("0")
            element("1", valueClassDescriptor)
            element<String>("2")
            element<Char>("3")
        }

        val encoder = SimpleListEncoder()

        val mappedIndexes = mapOf(
            0 to 1,
            1 to 0,
            2 to ReorderingCompositeEncoder.SKIP_ELEMENT_INDEX,
            3 to 2
        )
        ReorderingCompositeEncoder(3, encoder) { _, index -> mappedIndexes.getValue(index) }.apply {
            encodeDoubleElement(descriptor, 0, 17.0)
            encodeInlineElement(descriptor, 1).encodeString("Hello")
            encodeStringElement(descriptor, 2, "ignored")
            encodeCharElement(descriptor, 3, '!')
            endStructure(descriptor)
        }

        assertContentEquals(
            listOf("Hello", 17.0, '!'),
            encoder.output
        )
    }

    @Test
    fun shouldReorderWellWithSerializationPlugin() {
        val value = AllTypesExample(
            int = 1,
            nullableInt = null,
            intWrapped = IntValue(2),
            nullableIntWrapped = IntNullableValue(null),
            intWrappedNullable = IntValue(3),

            string = "Hello",
            nullableString = null,
            stringWrapped = StringValue("World"),
            nullableStringWrapped = StringNullableValue(null),
            stringWrappedNullable = StringValue("!!!"),

            boolean = true,
            nullableBoolean = null,
            booleanWrapped = BooleanValue(true),
            nullableBooleanWrapped = BooleanNullableValue(null),
            booleanWrappedNullable = BooleanValue(false),

            double = 1.0,
            nullableDouble = null,
            doubleWrapped = DoubleValue(2.0),
            nullableDoubleWrapped = DoubleNullableValue(null),
            doubleWrappedNullable = DoubleValue(3.0),

            nonReorderedList = listOf(1, 2, 3),
            nonReorderedNullableList = null,
            nonReorderedListWrapped = ListValue(listOf(4, 5, 6)),
            nonReorderedNullableListWrapped = ListNullableValue(null),
            nonReorderedListWrappedNullable = ListNullableValue(listOf(7, 8, 9)),
            nonReorderedListWrappedNullableValues = ListNullableValues(listOf(null, 8, 9)),

            float = 1.0f,
            nullableFloat = null,
            floatWrapped = FloatValue(2.0f),
            nullableFloatWrapped = FloatNullableValue(null),
            floatWrappedNullable = FloatValue(3.0f),

            long = 1L,
            nullableLong = null,
            longWrapped = LongValue(2L),
            nullableLongWrapped = LongNullableValue(null),
            longWrappedNullable = LongValue(3L),

            short = 1,
            nullableShort = null,
            shortWrapped = ShortValue(2),
            nullableShortWrapped = ShortNullableValue(null),
            shortWrappedNullable = ShortValue(3),

            nonReorderedSubStructure = SubStructure(
                a = IntValue(10),
                b = "Sub",
                c = null,
                d = null
            ),
            nonReorderedNullableSubStructure = null,
            nonReorderedSubStructureWrapped = SubStructureValue(
                SubStructure(
                    a = IntValue(11),
                    b = "Wrapped",
                    c = 12L,
                    d = 13.toByte()
                )
            ),
            nonReorderedNullableSubStructureWrapped = SubStructureNullableValue(null),
            nonReorderedSubStructureWrappedNullable = SubStructureValue(
                SubStructure(
                    a = IntValue(14),
                    b = "Nullable-Wrapped",
                    c = 15L,
                    d = 16.toByte()
                )
            ),

            byte = 1,
            nullableByte = null,
            byteWrapped = ByteValue(2),
            nullableByteWrapped = ByteNullableValue(null),
            byteWrappedNullable = ByteValue(3),

            char = 'A',
            nullableChar = null,
            charWrapped = CharValue('B'),
            nullableCharWrapped = CharNullableValue(null),
            charWrappedNullable = CharValue('C')
        )

        val output = StringBuilder()
        val encoder = LightJsonEncoder(
            output,
            descriptorToReorder = AllTypesExample.serializer().descriptor
        ) { descriptor, index -> descriptor.elementsCount - 1 - index }

        encoder.encodeSerializableValue(AllTypesExample.serializer(), value)

        // the final output should encode the fields in the reverse order, but should not reorder sub-structures
        assertEquals(
            actual = output.toString(),
            expected = """
{
  charWrappedNullable: "C",
  nullableCharWrapped: null,
  charWrapped: "B",
  nullableChar: null,
  char: "A",
  byteWrappedNullable: 3,
  nullableByteWrapped: null,
  byteWrapped: 2,
  nullableByte: null,
  byte: 1,
  nonReorderedSubStructureWrappedNullable: {
    a: 14,
    b: "Nullable-Wrapped",
    c: 15,
    d: 16
  },
  nonReorderedNullableSubStructureWrapped: null,
  nonReorderedSubStructureWrapped: {
    a: 11,
    b: "Wrapped",
    c: 12,
    d: 13
  },
  nonReorderedNullableSubStructure: null,
  nonReorderedSubStructure: {
    a: 10,
    b: "Sub",
    c: null,
    d: null
  },
  shortWrappedNullable: 3,
  nullableShortWrapped: null,
  shortWrapped: 2,
  nullableShort: null,
  short: 1,
  longWrappedNullable: 3,
  nullableLongWrapped: null,
  longWrapped: 2,
  nullableLong: null,
  long: 1,
  floatWrappedNullable: 3.0,
  nullableFloatWrapped: null,
  floatWrapped: 2.0,
  nullableFloat: null,
  float: 1.0,
  nonReorderedListWrappedNullableValues: [null,8,9],
  nonReorderedListWrappedNullable: [7,8,9],
  nonReorderedNullableListWrapped: null,
  nonReorderedListWrapped: [4,5,6],
  nonReorderedNullableList: null,
  nonReorderedList: [1,2,3],
  doubleWrappedNullable: 3.0,
  nullableDoubleWrapped: null,
  doubleWrapped: 2.0,
  nullableDouble: null,
  double: 1.0,
  booleanWrappedNullable: false,
  nullableBooleanWrapped: null,
  booleanWrapped: true,
  nullableBoolean: null,
  boolean: true,
  stringWrappedNullable: "!!!",
  nullableStringWrapped: null,
  stringWrapped: "World",
  nullableString: null,
  string: "Hello",
  intWrappedNullable: 3,
  nullableIntWrapped: null,
  intWrapped: 2,
  nullableInt: null,
  int: 1
}
        """.replace(Regex("""\s+"""), ""),
        )
    }
}

private class SimpleListEncoder(
    val output: MutableList<Any?> = mutableListOf(),
) : AbstractEncoder() {
    override val serializersModule: SerializersModule
        get() = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        output += value
    }
}

@Serializable
private data class AllTypesExample(
    val int: Int,
    val nullableInt: Int?,
    val intWrapped: IntValue,
    val nullableIntWrapped: IntNullableValue,
    val intWrappedNullable: IntValue?,

    val string: String,
    val nullableString: String?,
    val stringWrapped: StringValue,
    val nullableStringWrapped: StringNullableValue,
    val stringWrappedNullable: StringValue?,

    val boolean: Boolean,
    val nullableBoolean: Boolean?,
    val booleanWrapped: BooleanValue,
    val nullableBooleanWrapped: BooleanNullableValue,
    val booleanWrappedNullable: BooleanValue?,

    val double: Double,
    val nullableDouble: Double?,
    val doubleWrapped: DoubleValue,
    val nullableDoubleWrapped: DoubleNullableValue,
    val doubleWrappedNullable: DoubleValue?,

    val nonReorderedList: List<Int>,
    val nonReorderedNullableList: List<Int>?,
    val nonReorderedListWrapped: ListValue,
    val nonReorderedNullableListWrapped: ListNullableValue,
    val nonReorderedListWrappedNullable: ListNullableValue?,
    val nonReorderedListWrappedNullableValues: ListNullableValues?,

    val float: Float,
    val nullableFloat: Float?,
    val floatWrapped: FloatValue,
    val nullableFloatWrapped: FloatNullableValue,
    val floatWrappedNullable: FloatValue?,

    val long: Long,
    val nullableLong: Long?,
    val longWrapped: LongValue,
    val nullableLongWrapped: LongNullableValue,
    val longWrappedNullable: LongValue?,

    val short: Short,
    val nullableShort: Short?,
    val shortWrapped: ShortValue,
    val nullableShortWrapped: ShortNullableValue,
    val shortWrappedNullable: ShortValue?,

    val nonReorderedSubStructure: SubStructure,
    val nonReorderedNullableSubStructure: SubStructure?,
    val nonReorderedSubStructureWrapped: SubStructureValue,
    val nonReorderedNullableSubStructureWrapped: SubStructureNullableValue,
    val nonReorderedSubStructureWrappedNullable: SubStructureValue?,

    val byte: Byte,
    val nullableByte: Byte?,
    val byteWrapped: ByteValue,
    val nullableByteWrapped: ByteNullableValue,
    val byteWrappedNullable: ByteValue?,

    val char: Char,
    val nullableChar: Char?,
    val charWrapped: CharValue,
    val nullableCharWrapped: CharNullableValue,
    val charWrappedNullable: CharValue?,
)

@Serializable
private data class SubStructure(
    val a: IntValue,
    val b: String,
    val c: Long?,
    val d: Byte?,
)

@Serializable
@JvmInline
private value class IntValue(val value: Int)

@Serializable
@JvmInline
private value class StringValue(val value: String)

@Serializable
@JvmInline
private value class BooleanValue(val value: Boolean)

@Serializable
@JvmInline
private value class DoubleValue(val value: Double)

@Serializable
@JvmInline
private value class FloatValue(val value: Float)

@Serializable
@JvmInline
private value class LongValue(val value: Long)

@Serializable
@JvmInline
private value class ShortValue(val value: Short)

@Serializable
@JvmInline
private value class ByteValue(val value: Byte)

@Serializable
@JvmInline
private value class CharValue(val value: Char)

@Serializable
@JvmInline
private value class SubStructureValue(val value: SubStructure)

@Serializable
@JvmInline
private value class ListValue(val value: List<Int>)


@Serializable
@JvmInline
private value class IntNullableValue(val value: Int?)

@Serializable
@JvmInline
private value class StringNullableValue(val value: String?)

@Serializable
@JvmInline
private value class BooleanNullableValue(val value: Boolean?)

@Serializable
@JvmInline
private value class DoubleNullableValue(val value: Double?)

@Serializable
@JvmInline
private value class FloatNullableValue(val value: Float?)

@Serializable
@JvmInline
private value class LongNullableValue(val value: Long?)

@Serializable
@JvmInline
private value class ShortNullableValue(val value: Short?)

@Serializable
@JvmInline
private value class ByteNullableValue(val value: Byte?)

@Serializable
@JvmInline
private value class CharNullableValue(val value: Char?)

@Serializable
@JvmInline
private value class SubStructureNullableValue(val value: SubStructure?)

@Serializable
@JvmInline
private value class ListNullableValue(val value: List<Int>?)

@Serializable
@JvmInline
private value class ListNullableValues(val value: List<Int?>)


private class LightJsonEncoder(
    val sb: StringBuilder,
    val descriptorToReorder: SerialDescriptor,
    val mapElementIndex: (SerialDescriptor, Int) -> Int,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    var previousDescriptor: SerialDescriptor? = null

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (descriptor.kind == StructureKind.LIST) {
            sb.append('[')
        } else {
            sb.append('{')
        }
        previousDescriptor = null
        if (descriptor == descriptorToReorder) {
            return ReorderingCompositeEncoder(descriptor.elementsCount, this, mapElementIndex)
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (descriptor.kind == StructureKind.LIST) {
            sb.append(']')
        } else {
            sb.append('}')
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (previousDescriptor == null) {
            previousDescriptor = descriptor
        } else {
            previousDescriptor = descriptor
            sb.append(",")
        }
        if (descriptor.kind != StructureKind.LIST) {
            sb.append(descriptor.getElementName(index))
            sb.append(':')
        }
        return true
    }

    override fun encodeNull() {
        sb.append("null")
    }

    override fun encodeValue(value: Any) {
        sb.append(value)
    }

    override fun encodeString(value: String) {
        sb.append('"')
        sb.append(value)
        sb.append('"')
    }

    override fun encodeChar(value: Char) = encodeString(value.toString())
}
