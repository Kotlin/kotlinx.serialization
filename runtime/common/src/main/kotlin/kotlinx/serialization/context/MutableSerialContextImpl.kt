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

@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.KSerializer
import kotlinx.serialization.isInstance
import kotlin.reflect.KClass

private typealias SerializersMap = MutableMap<KClass<*>, KSerializer<*>>

/**
 * A default implementation of [MutableSerialContext]
 * which uses hash maps to store serializers associated with KClasses.
 *
 * Although it is rarely necessary to instantiate it in user code,
 * one may want to use it to implement their own context-aware
 * serialization format.
 */
class MutableSerialContextImpl(private val parentContext: SerialContext? = null): MutableSerialContext {

    private val classMap: SerializersMap = hashMapOf()

    private val polyMap: MutableMap<KClass<*>, SerializersMap> = hashMapOf()
    private val inverseClassNameMap: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()

    override fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap[forClass] = serializer
    }

    override fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        basePolyType: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>
    ) {
        val name = concreteSerializer.descriptor.name
        polyMap.getOrPut(basePolyType, ::hashMapOf)[concreteClass] = concreteSerializer
        inverseClassNameMap.getOrPut(basePolyType, ::hashMapOf)[name] = concreteSerializer
    }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? {
        if (!isInstance(basePolyType, obj)) return null
        (if (basePolyType == Any::class) StandardSubtypesOfAny.getSubclassSerializer(obj) else null)?.let { return it as KSerializer<out T> }
        return polyMap[basePolyType]?.get(obj::class) as? KSerializer<out T>
    }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        (if (basePolyType == Any::class) StandardSubtypesOfAny.getDefaultDeserializer(
            serializedClassName
        ) else null)?.let { return it as KSerializer<out T> }
        return inverseClassNameMap[basePolyType]?.get(serializedClassName) as? KSerializer<out T>
    }

    override fun <T: Any> get(kclass: KClass<T>): KSerializer<T>? = classMap[kclass] as? KSerializer<T>
            ?: parentContext?.get(kclass)
}
