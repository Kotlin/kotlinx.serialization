/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

/**
 * Specifies protobuf id (unique number for a field in protobuf message)
 * assigned to Kotlin property.
 *
 * See [https://developers.google.com/protocol-buffers/docs/proto#assigning-field-numbers]
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoId(public val id: Int)
