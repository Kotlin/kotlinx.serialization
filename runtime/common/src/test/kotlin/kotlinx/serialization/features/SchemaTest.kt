/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.test.*

@Serializable
data class Data1(@Optional val l: List<Int> = emptyList(), val s: String) {
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
data class Data2(@Optional val l: List<Int> = emptyList(), val s: String)


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
    @Optional val mm: Map<String, Data2>? = null
)

@Ignore // todo: unignore when corresponding features in plugin will be released
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
        assertEquals(listOf(IntDescriptor, StringDescriptor, ArrayListClassDesc(Data1.serializer().descriptor)), descs.take(3))
        val listListDesc = descs[3]
        assertFalse(listListDesc.isNullable)
        assertEquals(listListDesc.kind, StructureKind.LIST)
        assertEquals(1, listListDesc.elementsCount)
        assertEquals(BooleanDescriptor, listListDesc.elementDescriptors().first().elementDescriptors().first())
        val mapDesc = descs[4]
        assertTrue(mapDesc.isNullable)
        assertFalse(d.isElementOptional(4))
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
}
