/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals


class EnumDescriptorsTest {

    @Serializable
    enum class SerializableEnum {
        A,
        B
    }

    @SerialInfo
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    annotation class SerialAnnotation(val text: String)

    @SerialAnnotation("On Class")
    @Serializable
    enum class FullyAnnotatedEnum {
        @SerialAnnotation("On A")
        A,

        @SerialAnnotation("On B")
        B
    }

    @Serializable
    enum class EntriesAnnotatedEnum {
        @SerialAnnotation("On A")
        A,

        @SerialAnnotation("On B")
        B
    }

    @SerialAnnotation("On Class")
    @Serializable
    enum class ClassAnnotatedEnum {
        A,
        B
    }

    @Test
    fun testSerializableEnum() {
        val d = SerializableEnum.serializer().descriptor
        assertEquals("kotlinx.serialization.EnumDescriptorsTest.SerializableEnum", d.serialName)

        assertEquals("A", d.getElementName(0))
        assertEquals("B", d.getElementName(1))
    }

    @Test
    fun testFullyAnnotatedEnum() {
        assertFullyAnnotated(FullyAnnotatedEnum.serializer().descriptor)
        assertFullyAnnotated(serializer<FullyAnnotatedEnum>().descriptor)
    }

    @Test
    fun testEntriesAnnotatedEnum() {
        assertEntriesAnnotated(EntriesAnnotatedEnum.serializer().descriptor)
        assertEntriesAnnotated(serializer<EntriesAnnotatedEnum>().descriptor)
    }

    @Test
    fun testClassAnnotatedEnum() {
        assertClassAnnotated(ClassAnnotatedEnum.serializer().descriptor)
        assertClassAnnotated(serializer<ClassAnnotatedEnum>().descriptor)
    }

    private fun assertFullyAnnotated(descriptor: SerialDescriptor) {
        assertEquals(1, descriptor.annotations.size)
        assertEquals("On Class", (descriptor.annotations.first() as SerialAnnotation).text)

        assertEquals(1, descriptor.getElementAnnotations(0).size)
        assertEquals("On A", (descriptor.getElementAnnotations(0).first() as SerialAnnotation).text)

        assertEquals(1, descriptor.getElementAnnotations(1).size)
        assertEquals("On B", (descriptor.getElementAnnotations(1).first() as SerialAnnotation).text)
    }

    private fun assertEntriesAnnotated(descriptor: SerialDescriptor) {
        assertEquals(1, descriptor.getElementAnnotations(0).size)
        assertEquals("On A", (descriptor.getElementAnnotations(0).first() as SerialAnnotation).text)

        assertEquals(1, descriptor.getElementAnnotations(1).size)
        assertEquals("On B", (descriptor.getElementAnnotations(1).first() as SerialAnnotation).text)
    }

    private fun assertClassAnnotated(descriptor: SerialDescriptor) {
        assertEquals(1, descriptor.annotations.size)
        assertEquals("On Class", (descriptor.annotations.first() as SerialAnnotation).text)
    }

}
