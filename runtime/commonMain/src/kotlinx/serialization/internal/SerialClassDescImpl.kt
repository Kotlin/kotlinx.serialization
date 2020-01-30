/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

/**
 *
 */
@InternalSerializationApi
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    message = "Should not be used in general code, please migrate to SerialDescriptor() factory function instead",
    level = DeprecationLevel.ERROR
)
public open class SerialClassDescImpl(
    serialName: String,
    generatedSerializer: GeneratedSerializer<*>? = null,
    elementsCount: Int
) : PluginGeneratedSerialDescriptor(serialName, generatedSerializer, elementsCount)
