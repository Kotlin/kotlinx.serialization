/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JvmContextualGenericsTest : ContextualGenericsTest() {
    @Test
    fun testSurrogateSerializerFoundForGenericWithJavaType() {
        val filledBox = ThirdPartyBox(contents = Item("Foo"))
        val serializer = serializersModuleStatic.serializer(filledBox::class.java)
        assertEquals(boxWithItemSerializer.descriptor, serializer.descriptor)
    }

    @Test
    fun testSerializerFoundForContextualGenericWithJavaTypeToken() {
        val serializerA = serializersModuleWithProvider.serializer(typeTokenOf<ThirdPartyBox<Item>>())
        assertEquals(Item.serializer().descriptor, serializerA.descriptor.getElementDescriptor(0))
        val serializerB = serializersModuleWithProvider.serializer(typeTokenOf<ThirdPartyBox<AnotherItem>>())
        assertEquals(AnotherItem.serializer().descriptor, serializerB.descriptor.getElementDescriptor(0))
    }
}
