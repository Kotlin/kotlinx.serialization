/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlin.test.Test

class GenericOverrideTest: JsonTestBase() {

    @Serializable
    sealed class TypedSealedClass<T>(val a: T) {
        @Serializable
        @SerialName("child")
        data class Child(val y: Int) : TypedSealedClass<String>("10") {
            override fun toString(): String = "Child($a, $y)"
        }
    }

    @Test
    fun testAinChildSerializesAsString() = parametrizedTest { mode ->
        val encodedChild = """{"a":"10","y":42}"""
        assertJsonFormAndRestored(TypedSealedClass.Child.serializer(), TypedSealedClass.Child(42), encodedChild)
    }

    @Test
    fun testSerializeAsBaseClass() = parametrizedTest { mode ->
        val encodedChild = """{"type":"child","a":"10","y":42}"""
        assertJsonFormAndRestored(TypedSealedClass.serializer(String.serializer()), TypedSealedClass.Child(42), encodedChild)
    }
}
