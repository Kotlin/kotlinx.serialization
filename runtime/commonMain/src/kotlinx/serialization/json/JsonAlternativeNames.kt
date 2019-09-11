/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class JsonAlternativeNames(val names: Array<String>)

internal val JsonAlternativeNamesKey = DescriptorSchemaCache.Key<Map<String, Int>>()

internal fun SerialDescriptor.buildAlternativeNamesMap(): Map<String, Int> {
    fun MutableMap<String, Int>.putOrThrow(name: String, index: Int) {
        check(name !in this) {
            "Suggested name '$name' for property ${getElementName(index)} is already one of the names for property " +
                    "${getElementName(getValue(name))} in ${this@buildAlternativeNamesMap}"
        }
        this[name] = index
    }

    val builder = mutableMapOf<String, Int>()
    for (i in 0 until this.elementsCount) {
        builder.putOrThrow(getElementName(i), i)
        this.findAnnotation<JsonAlternativeNames>(i)?.names?.forEach { name ->
            builder.putOrThrow(name, i)
        }
    }
    return builder
}
