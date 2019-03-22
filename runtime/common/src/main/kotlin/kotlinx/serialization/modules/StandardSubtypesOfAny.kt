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
            makeNullable(
                PolymorphicSerializer(Any::class)
            )
        ),
        LinkedHashSet::class to LinkedHashSetSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            )
        ),
        HashSet::class to HashSetSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            )
        ),
        Set::class to LinkedHashSetSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            )
        ),
        LinkedHashMap::class to LinkedHashMapSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            ), makeNullable(PolymorphicSerializer(Any::class))
        ),
        HashMap::class to HashMapSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            ), makeNullable(PolymorphicSerializer(Any::class))
        ),
        Map::class to LinkedHashMapSerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            ), makeNullable(PolymorphicSerializer(Any::class))
        ),
        Map.Entry::class to MapEntrySerializer(
            makeNullable(
                PolymorphicSerializer(Any::class)
            ), makeNullable(PolymorphicSerializer(Any::class))
        ),
        String::class to StringSerializer,
        Char::class to CharSerializer,
        Double::class to DoubleSerializer,
        Float::class to FloatSerializer,
        Long::class to LongSerializer,
        Int::class to IntSerializer,
        Short::class to ShortSerializer,
        Byte::class to ByteSerializer,
        Boolean::class to BooleanSerializer,
        Unit::class to UnitSerializer
    )

    private val deserializingMap: Map<String, KSerializer<*>> = map.mapKeys { (_, s) -> s.descriptor.name }

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
