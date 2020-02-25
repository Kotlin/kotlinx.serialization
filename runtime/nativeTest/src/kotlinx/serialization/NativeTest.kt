/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlin.test.*

class CommonTest {
    @Test
    fun canSerialize() {
        val serializer = Shop.serializer()
        val jsonShop = Json.stringify(serializer, shop)
        assertTrue(jsonShop.isNotBlank())
    }

    @Test
    fun basicJson() {
        val serializer = SimpleData.serializer()
        val data = SimpleData("foo", 42)
        val json = Json.stringify(serializer, data)
        assertEquals("""{"foo":"foo","bar":42}""", json)
    }

    @Test
    fun nativeSupportSerialIds() {
        val country = CountryData.serializer()
        val id1 = country.descriptor.findAnnotation<Id>(0)?.id ?: 0
        val id2 = getSerialId(country.descriptor, 0)
        assertEquals(10, id1)
        assertEquals(10, id2)
    }

    @Test
    @OptIn(ImplicitReflectionSerializer::class)
    fun nativeSupportsSimpleReflectionSerializer() {
        val s = Json.stringify(shop)
        println(s)
        val shop2 = Json.parse<Shop>(s)
        assertEquals(shop, shop2)
    }
}
