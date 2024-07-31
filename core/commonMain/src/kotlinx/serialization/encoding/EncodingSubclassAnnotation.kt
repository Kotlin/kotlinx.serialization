/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

@RequiresOptIn(
    "You should implement Encoder or Decoder only if you want to write a custom kotlinx.serialization format. " +
        "Before doing so, please consult official guide at https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#custom-formats-experimental",
    level = RequiresOptIn.Level.WARNING
)
public annotation class AdvancedEncodingApi
