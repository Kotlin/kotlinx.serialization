/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import org.junit.Test
import kotlin.test.*

class SerializerJvmSpecificTest {

    enum class Foo

    @Serializable
    abstract class ExplicitAbstract(public val i: Int = 42)

    interface ImplicitInterface

    @Serializable(with = PolymorphicSerializer::class)
    interface ExplicitInterface

    @Serializable
    class Holder(
        val iif: ImplicitInterface,
        val eif: ExplicitInterface,
        val ea: ExplicitAbstract
    )


    @Test
    fun testNonSerializableEnum() {
        val serializer = serializer<Foo>()
        assertTrue(serializer.descriptor.kind is SerialKind.ENUM)
    }

    @Test
    fun testDefaultInterfaceSerializer() {
        assertEquals(holderChildDescriptor(0), serializer<ImplicitInterface>().descriptor)
    }

    @Test
    fun testExplicitInterfaceSerializer() {
        assertEquals(holderChildDescriptor(1), serializer<ExplicitInterface>().descriptor)
    }

    @Test
    fun testDefaultAbstractSerializer() {
        // TODO discuss on serializers equality
        assertEquals(holderChildDescriptor(2), serializer<ExplicitAbstract>().descriptor)
    }

    private fun holderChildDescriptor(i: Int) = Holder.serializer().descriptor.getElementDescriptor(i)
}
