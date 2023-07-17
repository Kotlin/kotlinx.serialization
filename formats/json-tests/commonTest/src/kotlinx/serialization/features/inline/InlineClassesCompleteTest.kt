/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INLINE_CLASSES_NOT_SUPPORTED", "SERIALIZER_NOT_FOUND")

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.Box
import kotlinx.serialization.test.*
import kotlin.jvm.*
import kotlin.test.*

@Serializable
@JvmInline
value class MyInt(val i: Int)

@Serializable
@JvmInline value class NullableMyInt(val i: Int?)

@Serializable
@JvmInline value class OverSerializable(val s: IntData)

@Serializable
@JvmInline value class OverSerializableNullable(val s: IntData?)

@Serializable
@JvmInline value class WithT<T>(val t: Box<T>)

@Serializable
@JvmInline value class WithTNullable<T>(val t: Box<T>?)

@Serializable
data class WithAll(
    val myInt: MyInt, // I
    val myIntNullable: MyInt?, // LMyInt;
    val nullableMyInt: NullableMyInt, //Ljava/lang/Integer;
    val nullableMyIntNullable: NullableMyInt?, // LNullableMyInt
    val overSerializable: OverSerializable, // LIntData;
    val overSerializableNullable: OverSerializable?, // LIntData;
    val nullableOverSerializable: OverSerializableNullable, // LIntData;
    val nullableOverSerializableNullable: OverSerializableNullable?, // LOverSerializableNullable;
    val withT: WithT<Int>, // LBox;
    val withTNullable: WithT<Int>?, // LBox
    val withNullableTNullable: WithT<Int?>?, // LBox;
    val withTNullableTNullable: WithTNullable<Int?>? // LWithTNullable;
)

@Serializable
data class WithGenerics(
    val myInt: Box<MyInt>,
    val myIntNullable: Box<MyInt?>,
    val nullableMyInt: Box<NullableMyInt>,
    val nullableMyIntNullable: Box<NullableMyInt?>,
    val overSerializable: Box<OverSerializable>,
    val overSerializableNullable: Box<OverSerializable?>,
    val nullableOverSerializable: Box<OverSerializableNullable>,
    val nullableOverSerializableNullable: Box<OverSerializableNullable?>,
    val boxInBox: Box<WithT<Int>>
)

class InlineClassesCompleteTest {
    @Test
    fun testAllVariantsWithoutNull() {
        val withAll = WithAll(
            MyInt(1),
            MyInt(2),
            NullableMyInt(3),
            NullableMyInt(4),
            OverSerializable(IntData(5)),
            OverSerializable(IntData(6)),
            OverSerializableNullable(IntData(7)),
            OverSerializableNullable(IntData(8)),
            WithT(Box(9)),
            WithT(Box(10)),
            WithT(Box(11)),
            WithTNullable(Box(12))
        )
        assertSerializedAndRestored(withAll, WithAll.serializer())
    }

    @Test
    fun testAllVariantsWithNull() {
        assertSerializedAndRestored(
            WithAll(
                MyInt(1),
                null,
                NullableMyInt(null),
                null,
                OverSerializable(IntData(5)),
                null,
                OverSerializableNullable(null),
                null,
                WithT(Box(9)),
                null,
                WithT(Box(null)),
                WithTNullable(Box(null))
            ), WithAll.serializer()
        )
    }

    @Test
    fun testAllGenericVariantsWithoutNull() {
        assertSerializedAndRestored(
            WithGenerics(
                Box(MyInt(1)),
                Box(MyInt(2)),
                Box(NullableMyInt(3)),
                Box(NullableMyInt(4)),
                Box(OverSerializable(IntData(5))),
                Box(OverSerializable(IntData(6))),
                Box(OverSerializableNullable(IntData(7))),
                Box(OverSerializableNullable(IntData(8))),
                Box(WithT(Box(9)))
            ), WithGenerics.serializer()
        )
    }

    @Test
    fun testAllGenericVariantsWithNull() {
        assertSerializedAndRestored(
            WithGenerics(
                Box(MyInt(1)),
                Box(null),
                Box(NullableMyInt(null)),
                Box(null),
                Box(OverSerializable(IntData(5))),
                Box(null),
                Box(OverSerializableNullable(null)),
                Box(null),
                Box(WithT(Box(9)))
            ), WithGenerics.serializer()
        )
    }
}
