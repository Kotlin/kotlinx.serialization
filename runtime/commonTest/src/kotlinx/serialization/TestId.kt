/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class Id(val id: Int)

public fun getSerialId(desc: SerialDescriptor, index: Int): Int?
        = desc.findAnnotation<Id>(index)?.id

public inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
    val candidates = getElementAnnotations(elementIndex).filterIsInstance<A>()
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw IllegalStateException("There are duplicate annotations of type ${A::class} in the descriptor $this")
    }
}
