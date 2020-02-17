/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.SampleEnum
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.test.EnumSerializer
import kotlin.Char.*
import kotlin.test.Test

class JsonNativePrimitivesTest : JsonTestBase() {
    @Test
    fun testTopLevelNativeInt() = assertJsonFormAndRestored(Int.serializer(), 42, "42", default)

    @Test
    fun testTopLevelNativeString() = assertJsonFormAndRestored(String.serializer(), "42", "\"42\"", default)

    @Test
    fun testTopLevelNativeChar() = assertJsonFormAndRestored(Char.serializer(), '4', "\"4\"", default)

    @Test
    fun testTopLevelNativeBoolean() = assertJsonFormAndRestored(Boolean.serializer(), true, "true", default)

    @Test
    fun testTopLevelNativeEnum() =
        assertJsonFormAndRestored(EnumSerializer("SampleEnum"), SampleEnum.OptionB, "\"OptionB\"", default)

    @Test
    fun testTopLevelNativeNullable() =
        assertJsonFormAndRestored(Int.serializer().nullable, null, "null", default)
}
