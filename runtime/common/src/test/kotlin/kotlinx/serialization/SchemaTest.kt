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

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.test.*

@Serializable
data class Data1(@Optional val l: List<Int> = emptyList(), val s: String) {
    @Serializer(forClass = Data1::class)
    companion object {
        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Data1") {
            init {
                addElement("l")
                pushDescriptor(ArrayListSerializer(IntSerializer).descriptor)
                addElement("s", true)
                pushDescriptor(StringSerializer.descriptor)
            }
        }
    }
}

class SchemaTest {
    @Test
    fun test() {
        val serialDescriptor: SerialDescriptor = Data1.serializer().descriptor
        val nested = serialDescriptor.getElementDescriptor(0)
        assertTrue(nested is ListLikeDesc)
        val elem = nested.getElementDescriptor(0)
        assertTrue(elem is PrimitiveDescriptor)
        assertEquals("kotlin.Int", elem.name)
        assertTrue(serialDescriptor.isElementOptional(1))
    }
}
