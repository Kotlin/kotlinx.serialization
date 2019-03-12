/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

@Deprecated(level = DeprecationLevel.WARNING, message = "Use Json methods instead")
class JsonTreeParser(private val input: String) {

    companion object {
        @Deprecated(level = DeprecationLevel.WARNING, message = "Use Json.parse instead", replaceWith = ReplaceWith("Json.plain.parseJson(input)"))
        fun parse(input: String): JsonObject = Json.plain.parseJson(input) as JsonObject
    }

    fun readFully(): JsonElement {
        @Suppress("DEPRECATION")
        return Json.plain.parseJson(input)
    }
}
