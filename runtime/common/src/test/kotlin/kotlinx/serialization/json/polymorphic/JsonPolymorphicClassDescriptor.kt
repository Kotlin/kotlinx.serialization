/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonPolymorphicClassDescriptor : JsonTestBase() {

    private val json = Json(unquoted = true, defaultClassDescriptor = "class").also { it.install(polymorphicTestModule) }

    @Test
    fun testPolymorphicProperties() = parametrizedTest { useStreaming ->
        val box = InnerBox(InnerImpl(42, "foo"))
        val string = json.stringify(InnerBox.serializer(), box, useStreaming)
        assertEquals("{base:{class:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}", string)
        assertEquals(box, json.parse(InnerBox.serializer(), string, useStreaming))
    }
}
