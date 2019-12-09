/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PLUGIN_ERROR")
@file:UseExperimental(ExperimentalUnsignedTypes::class)

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.UIntDescriptor
import kotlinx.serialization.internal.UIntSerializer
import kotlinx.serialization.test.assertStringFormAndRestored
import org.junit.Test

@Serializable
data class SimpleContainerForUInt(val i: UInt)

@Serializable(MyUIntSerializer::class)
inline class MyUInt(val m: Int)

@Serializer(forClass = MyUInt::class)
object MyUIntSerializer {
    override val descriptor = UIntDescriptor
    override fun serialize(encoder: Encoder, obj: MyUInt) {
        encoder.encodeInline(descriptor)?.encodeInt(obj.m)
    }

    override fun deserialize(decoder: Decoder): MyUInt {
        return MyUInt(decoder.decodeInline(descriptor).decodeInt())
    }
}

@Serializable
data class SimpleContainerForMyType(val i: MyUInt)

@Serializable
inline class MyList<T>(val list: List<T>)

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
    private val precedent: UInt = Int.MAX_VALUE.toUInt() + 10.toUInt()

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
    fun testSimpleContainerForMyTypeWithCustomSerializer() = assertStringFormAndRestored(
        """{"i":2147483657}""",
        SimpleContainerForMyType(MyUInt(precedent.toInt())),
        SimpleContainerForMyType.serializer(),
        printResult = true
    )

    @Test
    fun testSimpleContainerForList() = assertStringFormAndRestored(
        """{"i":[2147483657]}""",
        ContainerForList(MyList(listOf(precedent))),
        ContainerForList.serializer(UIntSerializer),
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
