package kotlinx.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.test.isJsLegacy
import kotlin.test.*


class InheritableSerialInfoTest {

    @InheritableSerialInfo
    annotation class InheritableDiscriminator(val discriminator: String)

    @InheritableDiscriminator("a")
    interface A

    @InheritableDiscriminator("a")
    interface B

    @InheritableDiscriminator("a")
    @Serializable
    abstract class C: A

    @Serializable
    sealed class D: C(), B

    @Serializable
    class E: D()

    @Serializable
    class E2: C()

    @Serializable
    class E3: A, B

    private fun doTest(descriptor: SerialDescriptor) {
        if (isJsLegacy()) return // Unsupported
        val list = descriptor.annotations.filterIsInstance<InheritableDiscriminator>()
        assertEquals(1, list.size)
        assertEquals("a", list.first().discriminator)
    }

    @Test
    fun testInheritanceFromSealed() = doTest(E.serializer().descriptor)
    @Test
    fun testInheritanceFromAbstract() = doTest(E2.serializer().descriptor)
    @Test
    fun testInheritanceFromInterface() = doTest(E3.serializer().descriptor)
}
