/*
 *  Copyright 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.json.JSON
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CustomContainerSerializersJVMTest {

    @Serializable
    data class Data(val a: Int, @Optional val b: Int = 42)

    data class Container<out T: Any>(val data: T)

    @Serializable
    data class OuterClass(
        val container: Container<Data>
    )

    class ContainerSerializer<T: Any>(val serializer: KSerializer<T>) : KSerializer<Container<T>> {
        override fun save(output: KOutput, obj: Container<T>) {
            serializer.save(output, obj.data)
        }

        override fun load(input: KInput): Container<T> {
            return Container(serializer.load(input))
        }

        override val serialClassDesc: KSerialClassDesc = serializer.serialClassDesc
    }

    private val obj = OuterClass(Container(Data(a = 24)))
    private lateinit var json: JSON

    @Before
    fun initContext() {
        val scope = SerialContext()
        scope.registerContainerSerializer(Container::class, ContainerSerializer::class)
        json = JSON(unquoted = true, context = scope)
    }

    @Test
    fun writeCustom() {
        val s = json.stringify(obj)
        assertEquals("{container:{a:24,b:42}}", s)
    }

    @Test
    fun readCustom() {
        val s = json.parse<OuterClass>("{container:{a:24,b:42}}")
        assertEquals(obj, s)
    }
}
