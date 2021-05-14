/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*

@ThreadLocal
private val jsonToCache: MutableMap<Json, DescriptorSchemaCache> = mutableMapOf()

/**
 * Emulate thread locality of cache on Native
 */
internal actual val Json.schemaCache: DescriptorSchemaCache
    get() = jsonToCache.getOrPut(this) { DescriptorSchemaCache() }
