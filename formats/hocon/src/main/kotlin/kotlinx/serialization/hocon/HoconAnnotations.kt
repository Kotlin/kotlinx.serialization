/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName

/**
 * This annotation has a higher priority than [SerialName] or [Hocon.useConfigNamingConvention].
 * This means that you have full control over property name for encoding and decoding in the HOCON format in practice.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class HoconName(val value: String)
