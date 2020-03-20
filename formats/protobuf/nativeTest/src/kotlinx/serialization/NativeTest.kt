/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.io.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.*
import kotlin.test.*

class CommonTest {

    @Test
    fun isomorphicProtobuf() {
        val country = russia
        val serial = CountryData.serializer()
        val country2 = ProtoBuf.load(serial, ProtoBuf.dump(serial, country))
        assertTrue(country !== country2)
        assertEquals(country, country2)
    }

    @Test
    fun nativeSupportSerialIds() {
        val country = CountryData.serializer()
        val id1 = country.descriptor.findAnnotation<ProtoId>(0)?.id ?: 0
        assertEquals(10, id1)
    }

    private inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
        return getElementAnnotations(elementIndex).find { it is A } as A?
    }

}
