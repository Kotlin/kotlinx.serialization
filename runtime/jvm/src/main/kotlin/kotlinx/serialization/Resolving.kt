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
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

// for user-defined external serializers
fun registerSerializer(forClassName: String, serializer: KSerializer<*>) {
    SerialCache.map.put(forClassName, serializer)
}

private fun mapJavaClassNameToKotlin(s: String): String = when (s) {
    "int", "java.lang.Integer" -> IntSerializer.serialClassDesc.name
    "boolean", "java.lang.Boolean" -> BooleanSerializer.serialClassDesc.name
    "byte", "java.lang.Byte" -> ByteSerializer.serialClassDesc.name
    "short", "java.lang.Short" -> ShortSerializer.serialClassDesc.name
    "long", "java.lang.Long" -> LongSerializer.serialClassDesc.name
    "float", "java.lang.Float" -> FloatSerializer.serialClassDesc.name
    "double", "java.lang.Double" -> DoubleSerializer.serialClassDesc.name
    "char", "java.lang.Character" -> CharSerializer.serialClassDesc.name
    "java.lang.String" -> StringSerializer.serialClassDesc.name
    "java.util.List", "java.util.ArrayList" -> ArrayListClassDesc.name
    "java.util.Set", "java.util.LinkedHashSet" -> LinkedHashSetClassDesc.name
    "java.util.HashSet" -> HashSetClassDesc.name
    "java.util.Map", "java.util.LinkedHashMap" -> LinkedHashMapClassDesc.name
    "java.util.HashMap" -> HashMapClassDesc.name
    "java.util.Map\$Entry" -> MapEntryClassDesc.name
    else -> s
}

fun <E> serializerByValue(value: E, context: SerialContext? = null): KSerializer<E> {
    val klass =
        (value as? Any)?.javaClass?.kotlin ?: throw SerializationException("Cannot determine class for value $value")
    return serializerByClass(klass, context)
}

fun <E> serializerBySerialDescClassname(className: String, context: SerialContext? = null): KSerializer<E> =
    SerialCache.lookupSerializer(className, context = context)

fun <E> serializerByClass(klass: KClass<*>, context: SerialContext? = null): KSerializer<E> =
    SerialCache.lookupSerializer(mapJavaClassNameToKotlin(klass.java.canonicalName ?: ""), klass, context)

@PublishedApi()
internal open class TypeBase<T>

inline fun <reified T> typeTokenOf(): Type {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!
    return (superType as ParameterizedType).actualTypeArguments.first()!!
}

// This method intended for static, format-agnostic resolving (e.g. in adapter factories) so context is not used here.
@Suppress("UNCHECKED_CAST")
fun serializerByTypeToken(type: Type): KSerializer<Any> = when (type) {
    is GenericArrayType -> {
        val eType = type.genericComponentType.let {
            when (it) {
                is WildcardType -> it.upperBounds.first()
                else -> it
            }
        }
        val serializer = serializerByTypeToken(eType)
        val kclass = when (eType) {
            is ParameterizedType -> (eType.rawType as Class<*>).kotlin
            is KClass<*> -> eType
            else -> throw IllegalStateException("unsupported type in GenericArray: ${eType::class}")
        } as KClass<Any>
        ReferenceArraySerializer<Any, Any>(kclass, serializer) as KSerializer<Any>
    }
    is Class<*> -> if (!type.isArray) {
        serializerByClass(type.kotlin)
    } else {
        val eType: Class<*> = type.componentType
        val s = serializerByTypeToken(eType)
        val arraySerializer = ReferenceArraySerializer<Any, Any>(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    }
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>)
        val args = (type.actualTypeArguments)
        when {
            List::class.java.isAssignableFrom(rootClass) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Set::class.java.isAssignableFrom(rootClass) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Map::class.java.isAssignableFrom(rootClass) -> HashMapSerializer(
                serializerByTypeToken(args[0]),
                serializerByTypeToken(args[1])
            ) as KSerializer<Any>
            Map.Entry::class.java.isAssignableFrom(rootClass) -> MapEntrySerializer(
                serializerByTypeToken(args[0]),
                serializerByTypeToken(args[1])
            ) as KSerializer<Any>

            else -> {
                val varargs = args.map { serializerByTypeToken(it) }.toTypedArray()
                (rootClass.invokeSerializerGetter(*varargs) as? KSerializer<Any>) ?: serializerByClass<Any>(
                    rootClass.kotlin
                )
            }
        }
    }
    is WildcardType -> serializerByTypeToken(type.upperBounds.first())
    else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $type ${type::class}")
}