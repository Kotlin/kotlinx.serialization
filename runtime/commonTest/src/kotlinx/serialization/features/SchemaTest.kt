/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.test.CommonEnumSerializer
import kotlinx.serialization.test.isJvm
import kotlin.test.*

@Serializable
data class Data1(val l: List<Int> = emptyList(), val s: String) {
    @Serializer(forClass = Data1::class)
    companion object {
        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Data1") {
            init {
                addElement("l", true)
                pushDescriptor(ArrayListSerializer(IntSerializer).descriptor)
                addElement("s")
                pushDescriptor(StringSerializer.descriptor)
            }
        }
    }
}

@Serializable
data class Data2(val l: List<Int> = emptyList(), val s: String)

@Serializable data class Box<T>(val boxed: T)

@Serializable data class BoxHolder(val stringBox: Box<String>, val intBox: Box<Int>)

@Serializable
data class DataZoo(
    @Transient val invisible: String = "",
    val a: Int,
    val b: String,
    val c: List<Data1>,
    val ll: List<List<Boolean>>,
    val m: Map<String, Data2>?
)

@Serializable
data class DataZooIsomorphic(
    @Transient val invisible: String = "",
    val b: Int,
    val a: String,
    val cc: List<Data1>,
    val lll: List<List<Boolean>>,
    val mm: Map<String, Data2>? = null
)

@Serializable
private data class DataWithEnum(val s: String, val enum: SampleEnum, val enumList: List<SampleEnum> = emptyList())

class SchemaTest {

    private fun checkDescriptor(serialDescriptor: SerialDescriptor) {
        val nested = serialDescriptor.getElementDescriptor(0)
        assertTrue(nested is ListLikeDescriptor)
        val elem = nested.getElementDescriptor(0)
        assertTrue(elem is PrimitiveDescriptor)
        assertEquals("kotlin.Int", elem.name)
        assertTrue(elem is IntDescriptor)
        assertTrue(serialDescriptor.isElementOptional(0))
    }

    @Test
    fun manualSchema() {
        checkDescriptor(Data1.serializer().descriptor)
    }

    @Test
    fun generatedSchema() {
        checkDescriptor(Data2.serializer().descriptor)
    }

    @Test
    fun richSchema() {
        val d: SerialDescriptor = DataZoo.serializer().descriptor
        val descs = d.elementDescriptors()
        assertEquals(5, descs.size)
        assertEquals(listOf<SerialDescriptor>(IntDescriptor, StringDescriptor, ArrayListClassDesc(Data1.serializer().descriptor)), descs.take(3))
        val listListDesc = descs[3]
        assertFalse(listListDesc.isNullable)
        assertEquals(listListDesc.kind, StructureKind.LIST)
        assertEquals(1, listListDesc.elementsCount)
        assertEquals(BooleanDescriptor, listListDesc.elementDescriptors().first().elementDescriptors().first())
        val mapDesc = descs[4]
        assertTrue(mapDesc.isNullable)
        assertFalse(d.isElementOptional(4), "Expected value to be marked as optional")
        assertEquals(2, mapDesc.elementsCount)
        assertEquals(listOf(StringDescriptor, Data2.serializer().descriptor), mapDesc.elementDescriptors())
    }

    @Test
    fun equalDescriptors() {
        val desc1: SerialDescriptor = DataZoo.serializer().descriptor
        val desc2: SerialDescriptor = DataZooIsomorphic.serializer().descriptor

        assertEquals(desc1.elementDescriptors(), desc2.elementDescriptors())
    }

    @Test
    fun genericDescriptors() {
        val boxes = BoxHolder.serializer().descriptor.elementDescriptors()
        assertTrue(boxes[0].getElementDescriptor(0) is StringDescriptor)
        assertTrue(boxes[1].getElementDescriptor(0) is IntDescriptor)
        assertNotEquals(boxes[0], boxes[1])
        val intBox = Box.serializer(IntSerializer).descriptor
        assertEquals(intBox, boxes[1])
    }

    @Test
    fun enumDescriptors() {
        val dataDescriptor = DataWithEnum.serializer().descriptor
        val enumDesc = dataDescriptor.getElementDescriptor(1)
        val serialName = if (isJvm()) "kotlinx.serialization.SampleEnum" else "SampleEnum"
        val manualSerializer = CommonEnumSerializer<SampleEnum>(serialName)

        assertEquals(enumDesc, manualSerializer.descriptor)
        assertEquals(enumDesc, dataDescriptor.getElementDescriptor(2).getElementDescriptor(0))
    }

    @Test
    fun kindNames() {
        val classDesc = BoxHolder.serializer().descriptor
        assertEquals("CLASS", classDesc.kind.toString())
        val intDesc = classDesc.elementDescriptors()[1].elementDescriptors()[0]
        assertEquals("INT", intDesc.kind.toString())
    }
}
