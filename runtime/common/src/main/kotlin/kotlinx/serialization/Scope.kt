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

    private val classMap: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()

    private val containersMap: MutableMap<KClass<*>, KClass<KSerializer<*>>> = hashMapOf()

    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap.put(forClass, serializer)
    }

    fun <T : Any> registerContainerSerializer(forClass: KClass<T>, serializer: KClass<out KSerializer<out T>>) {
        containersMap.put(forClass, serializer as KClass<KSerializer<*>>)
    }

    fun <T : Any> getSerializerByValue(value: T?): KSerializer<T>? = getSerializerByValue(value, null)

    internal fun <T : Any> getSerializerByValue(value: T?, containedSerializer: KSerializer<*>? = null): KSerializer<T>? {
        if (value == null) throw SerializationException("Cannot determine class for value $value")
        val t: T = value
        val klass = t::class
        return getSerializerByClass(klass, containedSerializer) as? KSerializer<T>
    }

    inline fun <reified T: Any> getSerializer(): KSerializer<T>? = getSerializerByClass(T::class)

    fun <T: Any> getSerializerByClass(klass: KClass<T>): KSerializer<T>? = getSerializerByClass(klass, null)

    internal fun <T : Any> getSerializerByClass(klass: KClass<T>, containedSerializer: KSerializer<*>? = null): KSerializer<T>? {
        if (containedSerializer == null) {
            return classMap[klass] as? KSerializer<T> ?: parentContext?.getSerializerByClass(klass)
        } else {
            val serializerClass = containersMap[klass] as? KClass<KSerializer<T>> ?: return parentContext?.getSerializerByClass(klass, containedSerializer)

            val requiredConstructor = serializerClass.constructors.find {
                it.parameters.size == 1 && it.parameters.first().type.classifier == KSerializer::class
            } ?: throw SerializationException("$serializerClass doesn't have constructor with single KSerializer parameter")
            return requiredConstructor.call(containedSerializer)
        }
    }
}

fun <T: Any> SerialContext?.klassSerializer(klass: KClass<T>) = this?.let { getSerializerByClass(klass) } ?: klass.serializer()
fun <T: Any> SerialContext?.valueSerializer(value: T) = this?.let { getSerializerByValue(value) } ?: value::class.serializer()

class ContextSerializer<T : Any>(
    val serializableClass: KClass<T>,
    val containedSerializer: KSerializer<*>? = null
) : KSerializer<T> {
    constructor(serializableClass: KClass<T>) : this(serializableClass, null)

    override fun save(output: KOutput, obj: T) {
        output.writeValue(obj, containedSerializer)
    }
    override fun load(input: KInput): T = input.readValue(serializableClass, containedSerializer)

    override val serialClassDesc: KSerialClassDesc
        get() = throw SerializationException("No descriptor")
}