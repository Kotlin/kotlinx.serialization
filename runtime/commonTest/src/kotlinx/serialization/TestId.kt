/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class Id(val id: Int)

public fun getSerialId(desc: SerialDescriptor, index: Int): Int?
        = desc.findAnnotation<Id>(index)?.id
