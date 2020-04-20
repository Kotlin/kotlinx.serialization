/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonDeserializePolymorphicTwiceTest {

    @Serializable
    sealed class Foo {
        @Serializable
        data class Bar(val a: Int) : Foo()
    }

    @Test
    fun testDeserializeTwice() {
        val json = Json.toJson(Foo.Bar(1) as Foo)
        assertEquals(Foo.Bar(1), Json.fromJson<Foo>(json))
        assertEquals(Foo.Bar(1), Json.fromJson<Foo>(json))
    }
}