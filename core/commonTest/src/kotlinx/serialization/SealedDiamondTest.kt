package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlin.test.*

class SealedDiamondTest {

    @Serializable
    sealed interface A {}

    @Serializable
    sealed interface B: A {}

    @Serializable
    sealed interface C: A {}

    @Serializable
    @SerialName("X")
    class X: B, C

    @Test
    fun testMultipleSuperSealedInterfaces() {
        val subclasses = A.serializer().descriptor.getElementDescriptor(1).elementDescriptors.map { it.serialName }
        assertEquals(listOf("X"), subclasses)
    }

}
