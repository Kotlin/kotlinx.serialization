/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonTreeDecoderPolymorphicTest : JsonTestBase() {

    @Serializable
    sealed class Sealed

    @Serializable
    data class ClassContainingItself(
        val a: String,
        val b: String,
        val c: ClassContainingItself? = null,
        val d: String?
    ) : Sealed()

    val inner = ClassContainingItself(
        "InnerA",
        "InnerB",
        null,
        "InnerC"
    )
    val outer = ClassContainingItself(
        "OuterA",
        "OuterB",
        inner,
        "OuterC"
    )

    @Test
    fun testDecodingWhenClassContainsItself() = parametrizedTest { jsonTestingMode ->
        val encoded = default.encodeToString(outer as Sealed, jsonTestingMode)
        val decoded: Sealed = Json.decodeFromString(encoded, jsonTestingMode)
        assertEquals(outer, decoded)
    }
}
