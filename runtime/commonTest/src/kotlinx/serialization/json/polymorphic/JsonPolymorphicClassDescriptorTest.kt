/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test

class JsonPolymorphicClassDescriptorTest : JsonTestBase() {

    private val json = Json {
        classDiscriminator = "class"
        serializersModule = polymorphicTestModule
    }

    @Test
    fun testPolymorphicProperties() = assertJsonFormAndRestored(
        InnerBox.serializer(),
        InnerBox(InnerImpl(42, "foo")),
        """{"base":{"class":"kotlinx.serialization.json.polymorphic.InnerImpl",""" +
                """"field":42,"str":"foo","nullable":null}}""",
        json
    )
}
