/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")

package kotlinx.io

import kotlinx.serialization.*

@Deprecated(message = message, level = DeprecationLevel.ERROR)
actual typealias IOException = java.io.IOException

@InternalSerializationApi
actual typealias OutputStream = java.io.OutputStream
@InternalSerializationApi
actual typealias ByteArrayOutputStream = java.io.ByteArrayOutputStream
