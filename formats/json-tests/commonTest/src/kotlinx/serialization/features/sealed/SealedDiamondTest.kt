package kotlinx.serialization.features.sealed

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SealedDiamondTest : JsonTestBase() {

    @Serializable
    sealed interface A {}

    @Serializable
    sealed interface B : A {}

    @Serializable
    sealed interface C : A {}

    @Serializable
    @SerialName("X")
    data class X(val i: Int) : B, C

    @Serializable
    @SerialName("Y")
    object Y : B, C

    @SerialName("E")
    enum class E : B, C {
        Q, W
    }

    @Test
    fun testMultipleSuperSealedInterfacesDescriptor() {
        val subclasses = A.serializer().descriptor.getElementDescriptor(1).elementDescriptors.map { it.serialName }
        assertEquals(listOf("E", "X", "Y"), subclasses)
    }

    @Test
    fun testMultipleSuperSealedInterfaces() {
        @Serializable
        data class Carrier(val a: A, val b: B, val c: C)
        assertJsonFormAndRestored(
            Carrier.serializer(),
            Carrier(X(1), X(2), Y),
            """{"a":{"type":"X","i":1},"b":{"type":"X","i":2},"c":{"type":"Y"}}"""
        )
    }

}
