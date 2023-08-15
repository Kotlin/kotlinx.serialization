/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*

@Suppress("DEPRECATION_ERROR")
internal actual val Json.schemaCache: DescriptorSchemaCache get() = this._schemaCache
