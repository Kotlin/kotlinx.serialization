/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.internal.DescriptorSchemaCache
import kotlin.native.concurrent.*

/**
 * Specifies an array of names those can be treated as alternative possible names
 * for the property during JSON parsing. Unlike [SerialName], does not affect JSON
 * writing in any way.
 *
 * To make actual use of this annotation, one should create [Json] instance
 * with [JsonBuilder.useAlternativeNames] flag
 * set to true.
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
        check(name !in this) {
            "The suggested name '$name' for property ${getElementName(index)} is already one of the names for property " +
                    "${getElementName(getValue(name))} in ${this@buildAlternativeNamesMap}"
        }
        this[name] = index
    }

    val builder = createMapForCache<String, Int>(elementsCount)
    for (i in 0 until elementsCount) {
        builder.putOrThrow(getElementName(i), i)
        getElementAnnotations(i).filterIsInstance<JsonNames>().singleOrNull()?.names?.forEach { name ->
            builder.putOrThrow(name, i)
        }
    }

    return if (builder.isEmpty()) emptyMap() else builder
}
