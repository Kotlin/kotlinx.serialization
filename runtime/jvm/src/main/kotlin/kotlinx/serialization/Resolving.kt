/*
 * Copyright 2017 JetBrains s.r.o.
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
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


// for user-defined external serializers
fun registerSerializer(forClassName: String, serializer: KSerializer<*>) {
    SerialCache.map.put(forClassName, serializer)
}

fun <E> serializerByValue(value: E, context: SerialContext? = null): KSerializer<E> {
    val klass = (value as? Any)?.javaClass?.kotlin ?: throw SerializationException("Cannot determine class for value $value")
    return serializerByClass(klass, context)
}

fun <E> serializerByClass(className: String, context: SerialContext? = null): KSerializer<E> = SerialCache.lookupSerializer(className, context = context)

fun <E> serializerByClass(klass: KClass<*>, context: SerialContext? = null): KSerializer<E> = SerialCache.lookupSerializer(klass.qualifiedName!!, klass, context)

// This method intended for static, format-agnostic resolving (e.g. in adapter factories) so context is not used here.
@Suppress("UNCHECKED_CAST")
fun serializerByTypeToken(type: Type): KSerializer<Any> = when(type) {
    is Class<*> -> if (!type.isArray) {
        serializerByClass(type.kotlin)
    } else {
        val eType: Class<*> = type.componentType
        val s = serializerByTypeToken(eType)
        ReferenceArraySerializer<Any, Any>(eType.kotlin as KClass<Any>, s) as KSerializer<Any>
    }
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>).kotlin
        val args = (type.actualTypeArguments)
        when {
            rootClass.isSubclassOf(List::class) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Set::class) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Map::class) -> HashMapSerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            rootClass.isSubclassOf(Map.Entry::class) -> MapEntrySerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            else -> serializerByClass(rootClass)
        }
    }
    else -> throw IllegalArgumentException("type should be instance of Class<?> or ParametrizedType")
}