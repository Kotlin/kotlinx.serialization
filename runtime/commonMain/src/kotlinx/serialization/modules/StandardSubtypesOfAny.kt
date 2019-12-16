/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.KClass

internal object StandardSubtypesOfAny {
    private val map: Map<KClass<*>, KSerializer<*>> = mapOf(
        List::class to ArrayListSerializer(
            PolymorphicSerializer(Any::class).nullable
        ),
        LinkedHashSet::class to LinkedHashSetSerializer(
            PolymorphicSerializer(Any::class).nullable
        ),
        HashSet::class to HashSetSerializer(
            PolymorphicSerializer(Any::class).nullable
        ),
        Set::class to LinkedHashSetSerializer(
            PolymorphicSerializer(Any::class).nullable
        ),
        LinkedHashMap::class to LinkedHashMapSerializer(
            PolymorphicSerializer(Any::class).nullable,
            PolymorphicSerializer(Any::class).nullable
        ),
        HashMap::class to HashMapSerializer(
            PolymorphicSerializer(Any::class).nullable,
            PolymorphicSerializer(Any::class).nullable
        ),
        Map::class to LinkedHashMapSerializer(
            PolymorphicSerializer(Any::class).nullable,
            PolymorphicSerializer(Any::class).nullable
        ),
        Map.Entry::class to MapEntrySerializer(
            PolymorphicSerializer(Any::class).nullable,
            PolymorphicSerializer(Any::class).nullable
        ),
        String::class to StringSerializer,
        Char::class to CharSerializer,
        Int::class to IntSerializer,
        Byte::class to ByteSerializer,
        Short::class to ShortSerializer,
        Long::class to LongSerializer,
        Double::class to DoubleSerializer,
        Float::class to FloatSerializer,
        Boolean::class to BooleanSerializer,
        Unit::class to UnitSerializer
    )

    private val deserializingMap: Map<String, KSerializer<*>> = map.mapKeys { (_, s) -> s.descriptor.serialName }

    @Suppress("UNCHECKED_CAST")
    internal fun getSubclassSerializer(objectToCheck: Any): KSerializer<*>? {
        for ((k, v) in map) {
            if (objectToCheck.isInstanceOf(k)) return v
        }
        return null
    }

    internal fun getDefaultDeserializer(serializedClassName: String): KSerializer<*>? =
        deserializingMap[serializedClassName]
}
