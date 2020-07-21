/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.modules.*

// Mirror of the dеprecated JsonConfiguration. Not for external use.
internal data class JsonConf(
    public val encodeDefaults: Boolean = true,
    public val ignoreUnknownKeys: Boolean = false,
    public val isLenient: Boolean = false,
    public val allowStructuredMapKeys: Boolean = false,
    public val prettyPrint: Boolean = false,
    public val prettyPrintIndent: String = "    ",
    public val coerceInputValues: Boolean = false,
    public val useArrayPolymorphism: Boolean = false,
    public val classDiscriminator: String = "type",
    public val allowSpecialFloatingPointValues: Boolean = false,
    public val serializersModule: SerializersModule = EmptySerializersModule)
