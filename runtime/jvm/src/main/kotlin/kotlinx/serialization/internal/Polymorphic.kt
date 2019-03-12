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

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import java.util.concurrent.*
import kotlin.reflect.KClass

internal object PolymorphicClassDesc : SerialClassDescImpl("kotlin.Any") {
    override val kind: SerialKind = UnionKind.POLYMORPHIC

    init {
        addElement("klass")
        pushAnnotation(SyntheticSerialId(1))
        addElement("value")
        pushAnnotation(SyntheticSerialId(2))
    }
}

@ImplicitReflectionSerializer
internal object ClassSerialCache {
    internal val map: Map<KClass<*>, KSerializer<*>> = mapOf(
            // not sure if we need collection serializer at all
//            Collection::class to ArrayListSerializer(makeNullable(PolymorphicSerializer)),
            List::class to ArrayListSerializer(makeNullable(PolymorphicSerializer)),
            HashSet::class to HashSetSerializer(makeNullable(PolymorphicSerializer)),
            Set::class to LinkedHashSetSerializer(makeNullable(PolymorphicSerializer)),
            HashMap::class to HashMapSerializer(makeNullable(PolymorphicSerializer), makeNullable(PolymorphicSerializer)),
            Map::class to LinkedHashMapSerializer(makeNullable(PolymorphicSerializer), makeNullable(PolymorphicSerializer)),
            Map.Entry::class to MapEntrySerializer(makeNullable(PolymorphicSerializer), makeNullable(PolymorphicSerializer))
    )

    @Suppress("UNCHECKED_CAST")
    internal fun getSubclassSerializer(klass: KClass<*>): KSerializer<*>? {
        if (klass.java.isArray) return ReferenceArraySerializer(Any::class, PolymorphicSerializer)
        for ((k, v) in map) {
            if (k.java.isAssignableFrom((klass.java))) return v
        }
        return null
    }
}

@ImplicitReflectionSerializer
internal object SerialCache {

    // Class fqn (Class.forName) to its serializer
    private val serializerCache: MutableMap<String, KSerializer<*>> = ConcurrentHashMap()

    private val allPrimitives: List<KSerializer<*>> = listOf(
        UnitSerializer, BooleanSerializer, ByteSerializer, ShortSerializer, IntSerializer,
        LongSerializer, FloatSerializer, DoubleSerializer, CharSerializer, StringSerializer)

    init {
        allPrimitives.forEach { registerSerializer(it.descriptor.name, it) }
        ClassSerialCache.map.values.toList().forEach { registerSerializer(it.descriptor.name, it) }
        registerSerializer("kotlin.Array", ReferenceArraySerializer(Any::class, PolymorphicSerializer))
    }

    internal fun registerSerializer(classFqn: String, serializer: KSerializer<*>) {
        serializerCache[classFqn] = serializer
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <E> lookupSerializer(className: String, kclass: KClass<*>? = null, context: SerialContext? = null): KSerializer<E> {
        return serializerCache.getOrPut(className) {
            loadSerializer(className, kclass, context)
        } as KSerializer<E>
    }

    private fun loadSerializer(className: String, kclass: KClass<*>? = null, context: SerialContext? = null): KSerializer<*> {
        val actualClass = kclass ?: Class.forName(className).kotlin
        val answer = context?.get(actualClass) ?: ClassSerialCache.getSubclassSerializer(actualClass)
        if (answer != null) return answer
        return requireNotNull(actualClass.serializer()) { "Can't found internal serializer for $actualClass" }
    }
}
