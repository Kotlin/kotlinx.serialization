/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.json.Json
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InvalidPolymorphicTypeTest {
    sealed interface InvalidType
    enum class InvalidEnum : InvalidType {
        A, B
    }
    @JvmInline
    value class InvalidInline(val value: Int) : InvalidType

    @Test
    fun invalidEnum() {
        assertFailsWith<IllegalArgumentException> {
            Json.encodeToString<InvalidType>(InvalidEnum.A)
        }
    }

    @Test
    fun invalidInline() {
        assertFailsWith<IllegalArgumentException> {
            Json.encodeToString<InvalidType>(InvalidInline(1))
        }
    }
}