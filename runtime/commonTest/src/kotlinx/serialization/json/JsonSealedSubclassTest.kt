/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals

sealed class Expr

@Serializable
data class Var(val id: String) : Expr()

class JsonSealedSubclassTest : JsonTestBase() {

    // inspired by kotlinx.serialization/#112
    @Test
    fun testCallSuperSealedConstructorProperly() = parametrizedTest { useStreaming ->
        val v1 = Var("a")
        val s1 = default.stringify(Var.serializer(), v1, useStreaming)// {"id":"a"}
        assertEquals("""{"id":"a"}""", s1)
        val v2: Var = default.parse(Var.serializer(), s1, useStreaming = true) // should not throw IllegalAccessError
        assertEquals(v1, v2)
    }
}
