/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonEncodingErrorMessagesTest: JsonTestBase() {

    @Serializable
    class DoubleData(val d: Double)

    @Test
    fun testEncodingFpMessage() = parametrizedTest { mode ->
        checkEncodingException(mode, {
            default.encodeToString(DoubleData(Double.NaN), mode)
        }) {
            // In streaming mode, key name is not available because we already have written it to the stream
            message("Unexpected special floating-point value NaN. By default, non-finite floating point values are prohibited because they do not conform JSON specification.",
                "Unexpected special floating-point value NaN with key d. By default, non-finite floating point values are prohibited because they do not conform JSON specification.")
            hint("JsonBuilder.allowSpecialFloatingPointValues = true")
            serialName(null)
        }
    }


}
