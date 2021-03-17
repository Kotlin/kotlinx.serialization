/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

// Mirror of the deprecated JsonConfiguration. Not for external use.
@OptIn(ExperimentalSerializationApi::class)
internal data class JsonConf(
    @JvmField val encodeDefaults: Boolean = false,
    @JvmField val ignoreUnknownKeys: Boolean = false,
    @JvmField val isLenient: Boolean = false,
    @JvmField val allowStructuredMapKeys: Boolean = false,
    @JvmField val prettyPrint: Boolean = false,
    @JvmField val prettyPrintIndent: String = "    ",
    @JvmField val coerceInputValues: Boolean = false,
    @JvmField val useArrayPolymorphism: Boolean = false,
    @JvmField val classDiscriminator: String = "type",
    @JvmField val allowSpecialFloatingPointValues: Boolean = false,
    @JvmField val serializersModule: SerializersModule = EmptySerializersModule
)
