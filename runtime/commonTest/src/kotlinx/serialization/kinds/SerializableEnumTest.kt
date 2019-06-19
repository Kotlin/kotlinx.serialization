/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.kinds

import kotlinx.serialization.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
@SerialName("custom_enum")
private enum class CustomEnum {
    @SerialName("foo_a")
    FooA,
    @SerialName("foo_b")
    @SerialId(10)
    FooB
}

@Serializable
private enum class One { A, B }

private enum class Two { A, B }

@Serializable
private data class TwoEnums(val one: One, val two: Two)

@Serializable
private data class WithCustomEnum(val c: CustomEnum)

class SerializableEnumTest {
    @Test
    fun serializedCorrectly() =
        assertStringFormAndRestored(
            """{"c":"foo_b"}""",
            WithCustomEnum(CustomEnum.FooB),
            WithCustomEnum.serializer(),
            printResult = true
        )

    @Test
    fun hasCorrectDescriptor() {
        val desc = WithCustomEnum.serializer().descriptor.getElementDescriptor(0)
        assertEquals("custom_enum", desc.name)
        assertEquals(listOf("foo_a", "foo_b"), desc.elementNames())
        assertEquals(desc, desc.getElementDescriptor(0))
        assertEquals(10, getSerialId(desc, 1))
    }

    @Test
    fun annotationDoesNotChangeEnum() = assertStringFormAndRestored(
        """{"one":"B","two":"B"}""",
        TwoEnums(One.B, Two.B),
        TwoEnums.serializer()
    )

    @Test
    fun annotationDoesNotChangeEnumDescriptor() {
        val descOne = TwoEnums.serializer().descriptor.getElementDescriptor(0)
        val descTwo = TwoEnums.serializer().descriptor.getElementDescriptor(1)
        assertEquals("kotlinx.serialization.kinds.Two", descTwo.name)
        assertEquals("kotlinx.serialization.kinds.One", descOne.name)
        assertEquals(descTwo.elementNames(), descOne.elementNames())
    }
}
