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

import kotlinx.serialization.KInput.Companion.READ_DONE
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomContainerSerializersTest {

    @Serializable
    data class Data(val a: Int, @Optional val b: Int = 42)

    data class Container<out T : Any>(val data: T)
    data class KeyValue<out K : Any, out V : Any>(val key: K, val value: V)

    @Serializable
    data class OuterClass(
        val container: Container<Data>,

        val keyValue: KeyValue<String, Data>
    )

    class ContainerSerializer<T : Any>(val serializer: KSerializer<T>) :
        KSerializer<Container<T>> {
        override fun save(output: KOutput, obj: Container<T>) {
            serializer.save(output, obj.data)
        }

        override fun load(input: KInput): Container<T> {
            return Container(serializer.load(input))
        }

        override val serialClassDesc: KSerialClassDesc = serializer.serialClassDesc

        companion object :
            KSerializerFactory<Container<*>> {
            override fun createSerializer(innerSerializers: List<KSerializer<*>>): KSerializer<Container<*>> {
                return ContainerSerializer(innerSerializers[0] as KSerializer<Any>)
            }
        }
    }

    class KeyValueSerializer<K : Any, V : Any>(
        val keySerializer: KSerializer<K>,
        val valueSerializer: KSerializer<V>
    ) : KSerializer<KeyValue<K, V>> {

        override fun save(output: KOutput, obj: KeyValue<K, V>) {
            @Suppress("NAME_SHADOWING")
            val output = output.writeBegin(serialClassDesc, keySerializer, valueSerializer)

            output.writeSerializableElementValue(serialClassDesc, 0, keySerializer, obj.key)
            output.writeSerializableElementValue(serialClassDesc, 1, valueSerializer, obj.value)

            output.writeEnd(serialClassDesc)
        }

        override fun load(input: KInput): KeyValue<K, V> {
            @Suppress("NAME_SHADOWING")
            val input = input.readBegin(serialClassDesc, keySerializer, valueSerializer)

            var key: K? = null
            var value: V? = null

            mainLoop@ while (true) {
                when (input.readElement(serialClassDesc)) {
                    READ_DONE -> {
                        break@mainLoop
                    }
                    0 -> {
                        key = input.readSerializableElementValue(serialClassDesc, 0, keySerializer)
                    }
                    1 -> {
                        value = input.readSerializableElementValue(serialClassDesc, 1, valueSerializer)
                    }
                }
            }

            input.readEnd(serialClassDesc)

            key ?: throw SerializationException("key is required")
            value ?: throw SerializationException("value is required")

            return KeyValue(key, value)
        }

        override val serialClassDesc: KSerialClassDesc =
            object : SerialClassDescImpl("kotlinx.serialization.CustomContainerSerializersTest.KeyValue") {
                init {
                    addElement("key")
                    addElement("value")
                }
            }

        companion object :
            KSerializerFactory<KeyValue<*, *>> {
            override fun createSerializer(innerSerializers: List<KSerializer<*>>): KSerializer<KeyValue<*, *>> {
                return KeyValueSerializer(
                    innerSerializers[0] as KSerializer<Any>,
                    innerSerializers[1] as KSerializer<Any>
                )
            }
        }
    }

    private val simpleObj = OuterClass(
        container = Container(Data(a = 24)),
        keyValue = KeyValue("test", Data(a = 111))
    )

    private val context = SerialContext().apply {
        registerSerializer(
            Container::class,
            ContainerSerializer
        )
        registerSerializer(
            KeyValue::class,
            KeyValueSerializer
        )
    }

    private val json: JSON = JSON(unquoted = true, context = context)

    @Test
    fun writeCustomSimple() {
        val s = json.stringify(simpleObj)
        assertEquals("{container:{a:24,b:42},keyValue:{key:test,value:{a:111,b:42}}}", s)
    }

    @Test
    fun readCustomSimple() {
        val s = json.parse<OuterClass>("{container:{a:24,b:42},keyValue:{key:test,value:{a:111,b:42}}}")
        assertEquals(simpleObj, s)
    }
}
