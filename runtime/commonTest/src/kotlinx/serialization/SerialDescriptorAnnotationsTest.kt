/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SerialDescriptorAnnotationsTest {

    @SerialInfo
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    annotation class CustomAnnotation(val value: String)

    @Serializable
    @SerialName("MyClass")
    @CustomAnnotation("onClass")
    data class WithNames(val a: Int, @CustomAnnotation("onProperty") val veryLongName: String)

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

    private fun List<Annotation>.getCustom() = filterIsInstance<CustomAnnotation>().single().value
}
