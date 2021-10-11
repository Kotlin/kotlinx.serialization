/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class JsonElementWrapper(val element: JsonElement)

@Serializable
data class JsonPrimitiveWrapper(val primitive: JsonPrimitive)

@Serializable
data class JsonNullWrapper(val element: JsonNull)

@Serializable
data class JsonObjectWrapper(val element: JsonObject)

@Serializable
data class JsonArrayWrapper(val array: JsonArray)