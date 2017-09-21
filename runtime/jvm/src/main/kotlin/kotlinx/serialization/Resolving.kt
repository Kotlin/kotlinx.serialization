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

fun <E> serializerByValue(value: E): KSerializer<E> {
    val klass = (value as? Any)?.javaClass?.kotlin ?: throw SerializationException("Cannot determine class for value $value")
    return serializerByClass(klass)
}

fun <E> serializerByClass(className: String): KSerializer<E> = SerialCache.lookupSerializer(className)

fun <E> serializerByClass(klass: KClass<*>): KSerializer<E> = SerialCache.lookupSerializer(klass.qualifiedName!!, klass)

fun serializerByTypeToken(type: Type): KSerializer<Any> = when(type) {
    is Class<*> -> serializerByClass(type.kotlin)
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>).kotlin
        val args = (type.actualTypeArguments)
        @Suppress("UNCHECKED_CAST")
        when {
            rootClass.isSubclassOf(List::class) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Set::class) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Map::class) -> HashMapSerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            rootClass.isSubclassOf(Map.Entry::class) -> MapEntrySerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            else -> serializerByClass(rootClass)
        }
    }
    else -> throw IllegalArgumentException()
}