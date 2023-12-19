/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

abstract class JsonClassDiscriminatorModeBaseTest(
    val discriminator: ClassDiscriminatorMode,
    val deserializeBack: Boolean = true
) : JsonTestBase() {

    @Serializable
    sealed class SealedBase

    @Serializable
    @SerialName("container")
    data class SealedContainer(val i: Inner): SealedBase()

    @Serializable
    @SerialName("inner")
    data class Inner(val x: String, val e: SampleEnum = SampleEnum.OptionB)

    @Serializable
    @SerialName("outer")
    data class Outer(val inn: Inner, val lst: List<Inner>, val lss: List<String>)

    data class ContextualType(val text: String)

    object CtxSerializer : KSerializer<ContextualType> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CtxSerializer") {
            element("a", String.serializer().descriptor)
            element("b", String.serializer().descriptor)
        }

        override fun serialize(encoder: Encoder, value: ContextualType) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.text.substringBefore("#"))
                encodeStringElement(descriptor, 1, value.text.substringAfter("#"))
            }
        }

        override fun deserialize(decoder: Decoder): ContextualType {
            lateinit var a: String
            lateinit var b: String
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (decodeElementIndex(descriptor)) {
                        0 -> a = decodeStringElement(descriptor, 0)
                        1 -> b = decodeStringElement(descriptor, 1)
                        else -> break
                    }
                }
            }
            return ContextualType("$a#$b")
        }
    }

    @Serializable
    @SerialName("withContextual")
    data class WithContextual(@Contextual val ctx: ContextualType, val i: Inner)

    val ctxModule = serializersModuleOf(CtxSerializer)

    val json = Json(default) {
        ignoreUnknownKeys = true
        serializersModule = polymorphicTestModule + ctxModule
        encodeDefaults = true
        classDiscriminatorMode = discriminator
    }

    @Serializable
    @SerialName("mixed")
    data class MixedPolyAndRegular(val sb: SealedBase, val sc: SealedContainer, val i: Inner)

    private inline fun <reified T> doTest(expected: String, obj: T) {
        parametrizedTest { mode ->
            val serialized = json.encodeToString(serializer<T>(), obj, mode)
            assertEquals(expected, serialized, "Failed with mode = $mode")
            if (deserializeBack) {
                val deserialized: T = json.decodeFromString(serializer(), serialized, mode)
                assertEquals(obj, deserialized, "Failed with mode = $mode")
            }
        }
    }

    fun testMixed(expected: String) {
        val i = Inner("in", SampleEnum.OptionC)
        val o = MixedPolyAndRegular(SealedContainer(i), SealedContainer(i), i)
        doTest(expected, o)
    }

    fun testIncludeNonPolymorphic(expected: String) {
        val o = Outer(Inner("X"), listOf(Inner("a"), Inner("b")), listOf("foo"))
        doTest(expected, o)
    }

    fun testIncludePolymorphic(expected: String) {
        val o = OuterNullableBox(OuterNullableImpl(InnerImpl(42), null), InnerImpl2(239))
        doTest(expected, o)
    }

    fun testIncludeSealed(expected: String) {
        val b = Box<SealedBase>(SealedContainer(Inner("x", SampleEnum.OptionC)))
        doTest(expected, b)
    }

    fun testContextual(expected: String) {
        val c = WithContextual(ContextualType("c#d"), Inner("x"))
        doTest(expected, c)
    }

    @Serializable
    @JsonClassDiscriminator("message_type")
    sealed class Base

    @Serializable // Class discriminator is inherited from Base
    sealed class ErrorClass : Base()

    @Serializable
    @SerialName("ErrorClassImpl")
    data class ErrorClassImpl(val msg: String) : ErrorClass()

    @Serializable
    @SerialName("Cont")
    data class Cont(val ec: ErrorClass, val eci: ErrorClassImpl)

    fun testCustomDiscriminator(expected: String) {
        val c = Cont(ErrorClassImpl("a"), ErrorClassImpl("b"))
        doTest(expected, c)
    }

    fun testTopLevelPolyImpl(expectedOpen: String, expectedSealed: String) {
        assertEquals(expectedOpen, json.encodeToString(InnerImpl(42)))
        assertEquals(expectedSealed, json.encodeToString(SealedContainer(Inner("x"))))
    }

    @Serializable
    @SerialName("NullableMixed")
    data class NullableMixed(val sb: SealedBase?, val sc: SealedContainer?)

    fun testNullable(expected: String) {
        val nm = NullableMixed(null, null)
       doTest(expected, nm)
    }
}
