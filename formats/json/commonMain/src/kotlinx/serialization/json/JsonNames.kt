/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.internal.*
import kotlin.native.concurrent.*

/**
 * Specifies an array of names those can be treated as alternative possible names
 * for the property during JSON decoding. Unlike [SerialName], does not affect JSON
 * encoding in any way.
 *
 * This annotation has lesser priority than [SerialName], even if there is a collision between them.
 *
 * @see JsonBuilder.useAlternativeNames
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class JsonNames(val names: Array<String>)

@SharedImmutable
internal val JsonAlternativeNamesKey = DescriptorSchemaCache.Key<Map<String, Int>>()

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.buildAlternativeNamesMap(): Map<String, Int> {
    fun MutableMap<String, Int>.putOrThrow(name: String, index: Int) {
        if (name in this) {
            throw JsonException(
                "The suggested name '$name' for property ${getElementName(index)} is already one of the names for property " +
                        "${getElementName(getValue(name))} in ${this@buildAlternativeNamesMap}"
            )
        }
        this[name] = index
    }

    var builder: MutableMap<String, Int>? = null
    for (i in 0 until elementsCount) {
        getElementAnnotations(i).filterIsInstance<JsonNames>().singleOrNull()?.names?.forEach { name ->
            if (builder == null) builder = createMapForCache(elementsCount)
            builder!!.putOrThrow(name, i)
        }
    }
    return builder ?: emptyMap()
}
