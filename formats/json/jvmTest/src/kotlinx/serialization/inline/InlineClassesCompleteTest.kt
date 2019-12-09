/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("PLUGIN_ERROR")

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.test.assertSerializedAndRestored
import org.junit.Test

@Serializable
inline class MyInt(val i: Int)

@Serializable
inline class NullableMyInt(val i: Int?)

@Serializable
inline class OverSerializable(val s: IntData)

@Serializable
inline class OverSerializableNullable(val s: IntData?)

@Serializable
inline class WithT<T>(val t: Box<T>)

@Serializable
inline class WithTNullable<T>(val t: Box<T>?)

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

// todo: add stirng representations
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
            WithT<Int>(Box(10)),
            WithT<Int?>(Box(11)),
            WithTNullable<Int?>(Box(12))
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
                WithT<Int?>(Box(null)),
                WithTNullable<Int?>(Box(null))
            ), WithAll.serializer()
        )
        // sounds good, doesnt work
        // todo: decide about nulls
        /*assertSerializedAndRestored(WithAllSmall(
                MyInt(1), null, NullableMyInt(null), NullableMyInt(null), OverSerializable(IntData(5)),  null, OverSerializableNullable(null), OverSerializableNullable(null)
        ), WithAllSmall.serializer())*/
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
