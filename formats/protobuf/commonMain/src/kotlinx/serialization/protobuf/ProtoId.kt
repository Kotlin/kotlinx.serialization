/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Deprecated(message = "SerialId is renamed to ProtoId to better reflect its semantics",
    level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("ProtoId(id)")
)
annotation class SerialId(val id: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoId(val id: Int)
