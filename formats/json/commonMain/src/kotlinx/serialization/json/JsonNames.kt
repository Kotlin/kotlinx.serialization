/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.internal.*
import kotlin.native.concurrent.*

/**
 * An annotation that indicates the field can be represented in JSON
 * with multiple possible alternative names.
 * [Json] format recognizes this annotation and is able to decode
 * the data using any of the alternative names.
 *
 * Unlike [SerialName] annotation, does not affect JSON encoding in any way.
 *
 * Example of usage:
 * ```
 * @Serializable
 * data class Project(@JsonNames("title") val name: String)
 *
 * val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
 * println(project)
 * val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
 * println(oldProject)
 * ```
 *
 * This annotation has lesser priority than [SerialName].
 *
 * @see JsonBuilder.useAlternativeNames
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class JsonNames(vararg val names: String)

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
