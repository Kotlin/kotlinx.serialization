/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class Id(val id: Int)
