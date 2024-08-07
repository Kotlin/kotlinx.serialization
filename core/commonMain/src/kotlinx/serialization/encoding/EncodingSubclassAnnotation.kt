/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

/**
 * Marks all encoding- and decoding-related interfaces in kotlinx.serialization.
 * These interfaces are used in serializers and have to be implemented only if you want to write
 * a custom serialization format. Since encoder/decoder invariants are quite complex,
 * it is recommended to start with reading their documentation: see [Encoder] and [Decoder],
 * and [kotlinx.serialization guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#custom-formats-experimental) about them.
 * There are also existing skeleton implementations that you may find useful: [AbstractEncoder] and [AbstractDecoder].
 */
@RequiresOptIn(
    "You should implement Encoder or Decoder only if you want to write a custom kotlinx.serialization format. " +
        "Before doing so, please consult official guide at https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#custom-formats-experimental",
    level = RequiresOptIn.Level.WARNING
)
public annotation class AdvancedEncodingApi
