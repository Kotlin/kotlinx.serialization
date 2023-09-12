package kotlinx.serialization

import kotlinx.serialization.test.*
import kotlin.test.*

class SerializersLookupInterfaceTest {

    interface I

    @Polymorphic
    interface I2

    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    @Serializable(PolymorphicSerializer::class)
    interface I3

    @Serializable
    @SerialName("S")
    sealed interface S

    // TODO: not working because (see #1207, plugin does not produce companion object for interfaces)
    // We even have #1853 with tests for that
    // @Serializable(ExternalSerializer::class)
    // interface External


    @Test
    fun testSealedInterfaceLookup() {
        val serializer = serializer<S>()
        assertTrue(serializer is SealedClassSerializer)
        assertEquals("S", serializer.descriptor.serialName)
    }

    @Test
    fun testInterfaceLookup() {
        // Native does not have KClass.isInterface
        if (isNative() || isWasm()) return

        val serializer1 = serializer<I>()
        assertTrue(serializer1 is PolymorphicSerializer)
        assertEquals("kotlinx.serialization.Polymorphic<I>", serializer1.descriptor.serialName)

        val serializer2 = serializer<I2>()
        assertTrue(serializer2 is PolymorphicSerializer)
        assertEquals("kotlinx.serialization.Polymorphic<I2>", serializer2.descriptor.serialName)

        val serializer3 = serializer<I3>()
        assertTrue(serializer3 is PolymorphicSerializer)
        assertEquals("kotlinx.serialization.Polymorphic<I3>", serializer3.descriptor.serialName)
    }
}
