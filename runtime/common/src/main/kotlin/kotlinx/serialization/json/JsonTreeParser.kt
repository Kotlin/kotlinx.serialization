/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

@Deprecated(level = DeprecationLevel.ERROR, message = "Use Json.parseJson instead")
class JsonTreeParser {

    companion object {
        fun parse(input: String): JsonObject = Json.plain.parseJson(input) as JsonObject
    }
}