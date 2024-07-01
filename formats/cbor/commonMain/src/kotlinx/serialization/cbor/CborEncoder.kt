/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

public interface CborEncoder {
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    public val cbor: Cbor
}