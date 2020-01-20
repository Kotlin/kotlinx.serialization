/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoId(val id: Int)
