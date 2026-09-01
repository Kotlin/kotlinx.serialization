/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.sealed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlin.jvm.JvmInline
import kotlin.test.Test

class SealedInterfacesInlineMapChildTest : JsonTestBase() {

    @Serializable
    sealed interface Parent {

        @JvmInline
        @Serializable
        @SerialName("child")
        value class Child(val value: Map<Int, String>) : Parent
    }

    @Test
    fun encodesDecodesInlineMapChildWithClassDiscriminator() {
        val value = Parent.Child(mapOf(1 to "one", 2 to "two"))
        assertJsonFormAndRestored(
            Parent.serializer(),
            value,
            """{"type":"child","1":"one","2":"two"}"""
        )
    }
}