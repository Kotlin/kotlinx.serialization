/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INLINE_CLASSES_NOT_SUPPORTED", "SERIALIZER_NOT_FOUND")
@file:OptIn(ExperimentalUnsignedTypes::class)

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.jvm.*
import kotlin.test.*

@Serializable
data class SimpleContainerForUInt(val i: UInt)

@Serializable(MyUIntSerializer::class)
@JvmInline
value class MyUInt(val m: Int)

@Serializer(forClass = MyUInt::class)
object MyUIntSerializer {
    override val descriptor = UInt.serializer().descriptor
    override fun serialize(encoder: Encoder, value: MyUInt) {
        encoder.encodeInline(descriptor).encodeInt(value.m)
    }

    override fun deserialize(decoder: Decoder): MyUInt {
        return MyUInt(decoder.decodeInline(descriptor).decodeInt())
    }
}

@Serializable
data class SimpleContainerForMyType(val i: MyUInt)

@Serializable
@JvmInline
value class MyList<T>(val list: List<T>)

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

@Serializable
@JvmInline
value class ResourceId(val id: String)

@Serializable
@JvmInline
value class ResourceType(val type: String)

@Serializable
data class ResourceIdentifier(val id: ResourceId, val type: ResourceType)

class InlineClassesTest : JsonTestBase() {
    private val precedent: UInt = Int.MAX_VALUE.toUInt() + 10.toUInt()

    @Test
    fun testSimpleContainer() = noLegacyJs {
        assertJsonFormAndRestored(
            SimpleContainerForUInt.serializer(),
            SimpleContainerForUInt(precedent),
            """{"i":2147483657}""",
        )
    }

    @Test
    fun testSimpleContainerForMyTypeWithCustomSerializer() = assertJsonFormAndRestored(
        SimpleContainerForMyType.serializer(),
        SimpleContainerForMyType(MyUInt(precedent.toInt())),
        """{"i":2147483657}""",
    )

    @Test
    fun testSimpleContainerForList() = noLegacyJs {
        assertJsonFormAndRestored(
            ContainerForList.serializer(UInt.serializer()),
            ContainerForList(MyList(listOf(precedent))),
            """{"i":[2147483657]}""",
        )
    }

    @Test
    fun testInlineClassesWithStrings() = noLegacyJs {
        assertJsonFormAndRestored(
            ResourceIdentifier.serializer(),
            ResourceIdentifier(ResourceId("resId"), ResourceType("resType")),
            """{"id":"resId","type":"resType"}"""
        )
    }

    @Test
    fun testUnsignedInBoxedPosition() = assertJsonFormAndRestored(
        UnsignedInBoxedPosition.serializer(),
        UnsignedInBoxedPosition(listOf(precedent)),
        """{"i":[2147483657]}""",
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
        assertJsonFormAndRestored(
            MixedPositions.serializer(),
            o,
            """{"int":-2147483639,"intNullable":-2147483639,"uint":2147483657,"uintNullable":2147483657,"boxedInt":[-2147483639],"boxedUInt":[2147483657],"boxedNullableInt":[null,-2147483639,null],"boxedNullableUInt":[null,2147483657,null]}""",
        )
    }
}
