/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonPolymorphicClassDescriptor : JsonTestBase() {

    private val json = Json {
        unquoted = true
        classDiscriminator = "class"
        serialModule = polymorphicTestModule
    }

    @Test
    fun testPolymorphicProperties() = parametrizedTest(
        InnerBox.serializer(),
        InnerBox(InnerImpl(42, "foo")),
        "{base:{class:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}",
        json
    )
}
