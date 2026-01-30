/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.JsonDecoder

internal interface PolymorphicJsonDecoder : JsonDecoder {
    val discriminator:String?
}