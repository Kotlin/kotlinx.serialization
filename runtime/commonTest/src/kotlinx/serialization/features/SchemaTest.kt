/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.test.EnumSerializer
import kotlin.test.*

class SchemaTest {

    @Serializable
    data class Data1(val l: List<Int> = emptyList(), val s: String) {
        @Serializer(forClass = Data1::class)
        companion object {
            // TODO removal of explicit type crashes the compiler
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Data1") {
                element("l", listSerialDescriptor<Int>(), isOptional = true)
                element("s", serialDescriptor<String>())
            }
        }
    }

    @Serializable
    data class Data2(val l: List<Int> = emptyList(), val s: String)

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

    private fun checkDescriptor(serialDescriptor: SerialDescriptor) {
        val nested = serialDescriptor.getElementDescriptor(0)
        assertTrue(nested is ListLikeDescriptor)
        val elem = nested.getElementDescriptor(0)
        assertTrue(elem.kind is PrimitiveKind.INT)
        assertEquals("kotlin.Int", elem.serialName)
        assertTrue(serialDescriptor.isElementOptional(0))
    }

    @Test
    fun testManualSchema() {
        checkDescriptor(Data1.serializer().descriptor)
    }

    @Test
    fun testGeneratedSchema() {
        checkDescriptor(Data2.serializer().descriptor)
    }

    @Test
    fun testRichSchema() {
        val d: SerialDescriptor = DataZoo.serializer().descriptor
        val descs = d.elementDescriptors.toList()
        assertEquals(5, descs.size)
        assertEquals(listOf(PrimitiveKind.INT, PrimitiveKind.STRING, StructureKind.LIST),
            descs.take(3).map { it.kind })
        val listListDesc = descs[3]
        assertFalse(listListDesc.isNullable)
        assertEquals(listListDesc.kind, StructureKind.LIST)
        assertEquals(1, listListDesc.elementsCount)
        assertEquals(PrimitiveKind.BOOLEAN, listListDesc.elementDescriptors.first().elementDescriptors.first().kind)
        val mapDesc = descs[4]
        assertTrue(mapDesc.isNullable)
        assertFalse(d.isElementOptional(4), "Expected value to be marked as optional")
        assertEquals(2, mapDesc.elementsCount)
        assertEquals(listOf(PrimitiveKind.STRING, StructureKind.CLASS), mapDesc.kinds())
    }

    @Test
    fun testEqualDescriptors() {
        val desc1: SerialDescriptor = DataZoo.serializer().descriptor
        val desc2: SerialDescriptor = DataZooIsomorphic.serializer().descriptor
        assertEquals(desc1.elementDescriptors.toList(), desc2.elementDescriptors.toList())
        assertEquals(Int.serializer().descriptor.elementDescriptors.toList(), Int.serializer().descriptor.elementDescriptors.toList())
    }

    @Test
    fun testGenericDescriptors() {
        val boxes = BoxHolder.serializer().descriptor.elementDescriptors.toList()
        assertTrue(boxes[0].getElementDescriptor(0).kind is PrimitiveKind.STRING)
        assertTrue(boxes[1].getElementDescriptor(0).kind is PrimitiveKind.INT)
        assertNotEquals(boxes[0], boxes[1])
        val intBox = Box.serializer(Int.serializer()).descriptor
        assertEquals(intBox.kind, boxes[1].kind)
    }

    @Test
    fun testEnumDescriptors() {
        val dataDescriptor = DataWithEnum.serializer().descriptor
        val enumDesc = dataDescriptor.getElementDescriptor(1)
        val serialName = "kotlinx.serialization.SampleEnum"
        val manualSerializer = EnumSerializer<SampleEnum>(serialName)
        assertEquals(enumDesc, manualSerializer.descriptor)
        assertEquals(enumDesc, dataDescriptor.getElementDescriptor(2).getElementDescriptor(0))
    }

    @Test
    fun testKindNames() {
        val classDesc = BoxHolder.serializer().descriptor
        assertEquals("CLASS", classDesc.kind.toString())
        val intDesc = classDesc.elementDescriptors.toList()[1].elementDescriptors.toList()[0]
        assertEquals("INT", intDesc.kind.toString())
    }

    private fun SerialDescriptor.kinds(): List<SerialKind> {
        return List(elementsCount) { getElementDescriptor(it).kind }
    }
}
