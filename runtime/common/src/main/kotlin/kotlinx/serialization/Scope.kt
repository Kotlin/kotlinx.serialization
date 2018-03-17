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

import kotlin.reflect.KClass


class SerialContext(private val parentContext: SerialContext? = null) {

    private val classMap: MutableMap<KClass<*>, KSerializerFactory<*>> = hashMapOf()

    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap.put(forClass, SimpleSerializerFactory(serializer))
    }

    fun <T : Any> registerSerializer(forClass: KClass<T>, serializerFactory: KSerializerFactory<*>) {
        classMap.put(forClass, serializerFactory)
    }

    fun <T : Any> getSerializerByValue(value: T?): KSerializer<T>? = getSerializerByValue(value, emptyList())

    internal fun <T : Any> getSerializerByValue(value: T?, innerSerializers: List<KSerializer<*>>): KSerializer<T>? {
        if (value == null) throw SerializationException("Cannot determine class for value $value")
        val t: T = value
        val klass = t::class
        return getSerializerByClass(klass, innerSerializers) as KSerializer<T>?
    }

    inline fun <reified T: Any> getSerializer(): KSerializer<T>? = getSerializerByClass(T::class)

    fun <T: Any> getSerializerByClass(klass: KClass<T>): KSerializer<T>? = getSerializerByClass(klass, emptyList())

    internal fun <T : Any> getSerializerByClass(klass: KClass<T>, innerSerializers: List<KSerializer<*>>): KSerializer<T>? {
        val factory = classMap[klass] ?: return parentContext?.getSerializerByClass(klass, innerSerializers)
        return factory.createSerializer(innerSerializers) as KSerializer<T>?
    }

    private class SimpleSerializerFactory<T>(val serializer: KSerializer<T>): KSerializerFactory<T> {
        override fun createSerializer(innerSerializers: List<KSerializer<*>>): KSerializer<T> {
            if (innerSerializers.isNotEmpty()) {
                throw SerializationException("Your serializer $serializer doesn't support inner serializers. Please use KSerializerFactory")
            }
            return serializer
        }
    }
}

fun <T: Any> SerialContext?.klassSerializer(klass: KClass<T>) = this?.let { getSerializerByClass(klass) } ?: klass.serializer()
fun <T: Any> SerialContext?.valueSerializer(value: T) = this?.let { getSerializerByValue(value) } ?: value::class.serializer()

class ContextSerializer<T : Any>(
    val serializableClass: KClass<T>,
    val innerSerializers: List<KSerializer<*>> = emptyList()
) : KSerializer<T> {
    constructor(serializableClass: KClass<T>) : this(serializableClass, emptyList())

    constructor(serializableClass: KClass<T>, vararg innerSerializers: KSerializer<*>) : this(serializableClass, listOf(*innerSerializers))

    constructor(serializableClass: KClass<T>, innerSerializer: KSerializer<*>) : this(serializableClass, listOf(innerSerializer))

    constructor(serializableClass: KClass<T>, serializer1: KSerializer<*>, serializer2: KSerializer<*>) : this(serializableClass, listOf(serializer1, serializer2))

    override fun save(output: KOutput, obj: T) {
        output.writeValue(obj, innerSerializers)
    }
    override fun load(input: KInput): T = input.readValue(serializableClass, innerSerializers)

    override val serialClassDesc: KSerialClassDesc
        get() = throw SerializationException("No descriptor")
}