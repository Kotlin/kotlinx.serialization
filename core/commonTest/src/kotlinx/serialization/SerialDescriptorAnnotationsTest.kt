/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlin.test.*

class SerialDescriptorAnnotationsTest {

    @SerialInfo
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    annotation class CustomAnnotation(val value: String)

    @Serializable
    @SerialName("MyClass")
    @CustomAnnotation("onClass")
    data class WithNames(
        val a: Int,
        @CustomAnnotationWithDefault @CustomAnnotation("onProperty") val veryLongName: String
    )

    @SerialInfo
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    annotation class CustomAnnotationWithDefault(val value: String = "foo")

    @SerialInfo
    @Target(AnnotationTarget.PROPERTY)
    public annotation class JShort(val order: SByteOrder = SByteOrder.BE, val mod: SByteMod = SByteMod.Add)

    public enum class SByteOrder {
        BE, LE
    }

    public enum class SByteMod {
        None, Add
    }

    @Serializable
    public class Foo(
        @JShort(SByteOrder.LE, SByteMod.None) public val bar: Short,
        @JShort public val baz: Short
    )


    @Test
    fun testSerialNameOnClass() {
        val desc = WithNames.serializer().descriptor
        val name = desc.serialName
        assertEquals("MyClass", name)
    }

    @Test
    fun testCustomSerialAnnotationOnProperty() {
        val desc: SerialDescriptor = WithNames.serializer().descriptor
        val b = desc.getElementAnnotations(1).getCustom()
        assertEquals("onProperty", b)
    }

    @Test
    fun testCustomSerialAnnotationOnClass() {
        val desc: SerialDescriptor = WithNames.serializer().descriptor
        val name = desc.annotations.getCustom()
        assertEquals("onClass", name)
    }

    @Test
    fun testCustomAnnotationWithDefaultValue() {
        val value =
            WithNames.serializer().descriptor
                .getElementAnnotations(1).filterIsInstance<CustomAnnotationWithDefault>().single()
        assertEquals("foo", value.value)
    }

    @Test
    fun testAnnotationWithMultipleArgs() {
        fun SerialDescriptor.getValues(i: Int) = getElementAnnotations(i).filterIsInstance<JShort>().single().run { order to mod }
        assertEquals(SByteOrder.LE to SByteMod.None, Foo.serializer().descriptor.getValues(0))
        assertEquals(SByteOrder.BE to SByteMod.Add, Foo.serializer().descriptor.getValues(1))
    }

    private fun List<Annotation>.getCustom() = filterIsInstance<CustomAnnotation>().single().value

    @Serializable
    @CustomAnnotation("sealed")
    sealed class Result {
        @Serializable class OK(val s: String): Result()
    }

    @Serializable
    @CustomAnnotation("abstract")
    abstract class AbstractResult {
        var result: String = ""
    }

    @Serializable
    @CustomAnnotation("object")
    object ObjectResult {}

    @Serializable
    class Holder(val r: Result, val a: AbstractResult, val o: ObjectResult, @Contextual val names: WithNames)

    private fun doTest(position: Int, expected: String) {
        val desc = Holder.serializer().descriptor.getElementDescriptor(position)
        assertEquals(expected, desc.annotations.getCustom())
    }

    @Test
    fun testCustomAnnotationOnSealedClass() = doTest(0, "sealed")

    @Test
    fun testCustomAnnotationOnPolymorphicClass() = doTest(1, "abstract")

    @Test
    fun testCustomAnnotationOnObject() = doTest(2, "object")

    @Test
    fun testCustomAnnotationTransparentForContextual() = doTest(3, "onClass")
}
