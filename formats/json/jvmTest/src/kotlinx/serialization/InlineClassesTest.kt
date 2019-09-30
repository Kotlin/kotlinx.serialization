@file:Suppress("PLUGIN_ERROR")
@file:UseExperimental(ExperimentalUnsignedTypes::class)

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.test.assertStringFormAndRestored
import org.junit.Test

@Serializable
data class SimpleContainerForUInt(val i: UInt)

@Serializable(MyIntSerializer::class)
inline class MyInt(val m: Int)

@Serializer(forClass = MyInt::class)
object MyIntSerializer {
    override val descriptor = UIntDescriptor
    override fun serialize(encoder: Encoder, obj: MyInt) {
        error("inline class should not be boxed for serialization")
    }

    override fun deserialize(decoder: Decoder): MyInt {
        error("inline class should not be boxed for serialization")
    }
}


@Serializable
data class SimpleContainerForMyType(val i: MyInt)

@Serializable(MyListSerializer::class)
inline class MyList<T>(val list: List<T>)

@Serializer(forClass = MyList::class)
class MyListSerializer<T>(val tSerializer: KSerializer<T>) : KSerializer<MyList<T>> {
    override val descriptor = ArrayListClassDesc(tSerializer.descriptor)
    override fun serialize(encoder: Encoder, obj: MyList<T>) {
        error("inline class should not be boxed for serialization")
    }

    override fun deserialize(decoder: Decoder): MyList<T> {
        error("inline class should not be boxed for serialization")
    }
}

@Serializable
data class ContainerForList<T>(val i: MyList<T>)

@Serializable
data class UnsignedInBoxedPosition(val i: List<UInt>)

@Serializable
data class MixedPositions(
    val int: Int,
    val intNullable: Int?,
    val uint: UInt,
    val uintNullable: UInt?,
    val boxedInt: List<Int>,
    val boxedUInt: List<UInt>,
    val boxedNullableInt: List<Int?>,
    val boxedNullableUInt: List<UInt?>
)

class InlineClassesTest {
    private val precedent = Int.MAX_VALUE.toUInt() + 10.toUInt()

    @Test
    fun testSimpleContainer() {

        assertStringFormAndRestored(
            """{"i":2147483657}""",
            SimpleContainerForUInt(precedent),
            SimpleContainerForUInt.serializer(),
            printResult = true
        )
    }

    @Test
    fun testSimpleContainerForMyType() = assertStringFormAndRestored(
        """{"i":2147483657}""",
        SimpleContainerForMyType(MyInt(precedent.toInt())),
        SimpleContainerForMyType.serializer(),
        printResult = true
    )

    @Test
    fun testSimpleContainerForList() = assertStringFormAndRestored(
        """{"i":[-2147483639]}""",
        ContainerForList(MyList(listOf(precedent.toInt()))),
        ContainerForList.serializer(IntSerializer),
        printResult = true
    )

    @Test
    fun testUnsignedInBoxedPosition() = assertStringFormAndRestored(
        """{"i":[2147483657]}""",
        UnsignedInBoxedPosition(listOf(precedent)),
        UnsignedInBoxedPosition.serializer(),
        printResult = true
    )

    @Test
    fun testMixedPositions() {
        val o = MixedPositions(
            int = precedent.toInt(),
            intNullable = precedent.toInt(),
            uint = precedent,
            uintNullable = precedent,
            boxedInt = listOf(precedent.toInt()),
            boxedUInt = listOf(precedent),
            boxedNullableInt = listOf(null, precedent.toInt(), null),
            boxedNullableUInt = listOf(null, precedent, null)
        )
        assertStringFormAndRestored(
            """{"int":-2147483639,"intNullable":-2147483639,"uint":2147483657,"uintNullable":2147483657,"boxedInt":[-2147483639],"boxedUInt":[2147483657],"boxedNullableInt":[null,-2147483639,null],"boxedNullableUInt":[null,2147483657,null]}""",
            o,
            MixedPositions.serializer(),
            printResult = true
        )
    }
}
