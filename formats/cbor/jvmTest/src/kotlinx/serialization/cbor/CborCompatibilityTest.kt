/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import com.upokecenter.cbor.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import org.junit.Test
import kotlin.test.*

class CborCompatibilityTest {

    @Serializable
    data class SomeClass(val prop: Int = 0)

    @Serializable
    data class WithMap(val map: Map<Long, Long>)
    @Serializable
    data class IntData(val intV: Int)

    @Serializable
    data class StringData(val data: String)

    @Serializable
    data class SomeComplexClass<T>(
        val boxed: T,
        val otherClass: StringData,
        val primitive: Int,
        val map: Map<String, IntData>
    )

    private inline fun <reified T> compare(obj: T, serializer: KSerializer<T>) {
        val bytes = CBORObject.FromObject(obj).EncodeToBytes()
        assertEquals(obj, Cbor.decodeFromByteArray(serializer, bytes))
    }

    @Test
    fun basicClassFromAnotherLibrary() {
        compare(SomeClass(), SomeClass.serializer())
    }

    @Test
    fun basicListFromAnotherLibrary() {
        compare(
            listOf(
                SomeClass(1),
                SomeClass(2),
                SomeClass(3)
            ), ListSerializer(SomeClass.serializer())
        )
    }

    @Test
    fun withMap() {
        compare(WithMap(mapOf()), WithMap.serializer())
        compare(WithMap(mapOf(10L to 10L)), WithMap.serializer())
        compare(
            WithMap(
                mapOf(
                    10L to 10L,
                    20L to 20L
                )
            ), WithMap.serializer())
    }

    @Test
    fun someComplexClass() {
        val obj = SomeComplexClass(
            listOf(10),
            StringData("20"),
            30,
            mapOf("40" to IntData(40), "50" to IntData(50))
        )
        val serial = SomeComplexClass.serializer(ListSerializer(Int.serializer()))
        compare(obj, serial)
    }
}
