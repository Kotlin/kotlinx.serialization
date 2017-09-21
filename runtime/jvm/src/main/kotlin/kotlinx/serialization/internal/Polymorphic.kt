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

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf

object PolymorphicClassDesc : SerialClassDescImpl("kotlin.Any") {
    override val kind: KSerialClassKind = KSerialClassKind.POLYMORPHIC

    init {
        addElement("klass")
        addElement("value")
    }
}

object PolymorphicSerializer : KSerializer<Any> {

    override val serialClassDesc: KSerialClassDesc
        get() = PolymorphicClassDesc

    override fun save(output: KOutput, obj: Any) {
        val saver = serializerByValue(obj)
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc)
        output.writeStringElementValue(serialClassDesc, 0, saver.serialClassDesc.name)
        output.writeSerializableElementValue(serialClassDesc, 1, saver, obj)
        output.writeEnd(serialClassDesc)
    }

    override fun load(input: KInput): Any {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (input.readElement(serialClassDesc)) {
                KInput.READ_ALL -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                    val loader = serializerByClass<Any>(klassName)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                    break@mainLoop
                }
                KInput.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = serializerByClass<Any>(klassName)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.readEnd(serialClassDesc)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }
}


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
        if (klass.java.isArray) return ReferenceArraySerializer(Any::class, PolymorphicSerializer as KSerializer<Any?>)
        for ((k, v) in map) {
            if (klass.isSubclassOf(k)) return v
        }
        return null
    }
}

internal val allPrimitives: List<KSerializer<*>> = listOf(
        UnitSerializer, BooleanSerializer, ByteSerializer, ShortSerializer, IntSerializer,
        LongSerializer, FloatSerializer, DoubleSerializer, CharSerializer, StringSerializer
)

internal object SerialCache {
    internal val map: MutableMap<String, KSerializer<*>> = HashMap()

    init {
        allPrimitives.forEach { registerSerializer(it.serialClassDesc.name, it) }
        ClassSerialCache.map.values.toList().forEach { registerSerializer(it.serialClassDesc.name, it) }
        @Suppress("UNCHECKED_CAST")
        registerSerializer("kotlin.Array", ReferenceArraySerializer(Any::class, PolymorphicSerializer as KSerializer<Any?>))
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <E> lookupSerializer(className: String, preloadedClass: KClass<*>? = null): KSerializer<E> {
        // First, look in the map
        var ans = map[className]
        if (ans != null) return ans as KSerializer<E>
        // If it's not there, maybe it came from java
        val klass = preloadedClass ?: Class.forName(className).kotlin
        ans = ClassSerialCache.getSubclassSerializer(klass)
        if (ans != null) return ans as KSerializer<E>
        // Then, it's user defined class
        val last = klass.companionObjectInstance as? KSerializer<E>
        return requireNotNull(last) { "Can't found internal serializer for class $klass" }
    }
}