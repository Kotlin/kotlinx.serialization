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

package kotlinx.serialization.context

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
            if (isInstance(k, objectToCheck)) return v
        }
        return null
    }

    internal fun getDefaultDeserializer(serializedClassName: String): KSerializer<*>? =
        deserializingMap[serializedClassName]
}
