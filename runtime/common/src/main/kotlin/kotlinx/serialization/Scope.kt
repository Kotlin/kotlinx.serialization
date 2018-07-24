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

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class SerialContext(private val parentContext: SerialContext? = null) {

    private val classMap: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()

    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap.put(forClass, serializer)
    }

    fun <T: Any> KClass<T>.getSerializer(): KSerializer<T>? = getSerializerByClass(this)

    fun <T : Any> getSerializerByValue(value: T?): KSerializer<T>? {
        if (value == null) throw SerializationException("Cannot determine class for value $value")
        val t: T = value
        val klass = t::class
        return getSerializerByClass(klass) as? KSerializer<T>
    }

    inline fun <reified T: Any> getSerializer(): KSerializer<T>? = getSerializerByClass(T::class)

    fun <T: Any> getSerializerByClass(klass: KClass<T>): KSerializer<T>? = classMap[klass] as? KSerializer<T> ?: parentContext?.getSerializerByClass(klass)
}

fun <T: Any> SerialContext?.klassSerializer(klass: KClass<T>) = this?.let { getSerializerByClass(klass) } ?: klass.serializer()
fun <T: Any> SerialContext?.valueSerializer(value: T) = this?.let { getSerializerByValue(value) } ?: value::class.serializer()

class ContextSerializer <T : Any> (val serializableClass: KClass<T>) : KSerializer<T> {
    override fun serialize(output: Encoder, obj: T) {
        output.encodeValue(obj)
    }
    override fun deserialize(input: Decoder): T = input.decodeValue(serializableClass)

    override val descriptor: SerialDescriptor
        get() = PrimitiveDesc("CONTEXT") //todo: remove this crutch
}
