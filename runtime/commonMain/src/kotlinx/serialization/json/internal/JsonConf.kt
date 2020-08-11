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
    @JvmField public val encodeDefaults: Boolean = true,
    @JvmField public val ignoreUnknownKeys: Boolean = false,
    @JvmField public val isLenient: Boolean = false,
    @JvmField public val allowStructuredMapKeys: Boolean = false,
    @JvmField public val prettyPrint: Boolean = false,
    @JvmField public val prettyPrintIndent: String = "    ",
    @JvmField public val coerceInputValues: Boolean = false,
    @JvmField public val useArrayPolymorphism: Boolean = false,
    @JvmField public val classDiscriminator: String = "type",
    @JvmField public val allowSpecialFloatingPointValues: Boolean = false,
    @JvmField public val serializersModule: SerializersModule = EmptySerializersModule
)
