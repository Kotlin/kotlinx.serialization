/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class SampleTestsJVM {
    @Test
    fun testHello() {
        assertTrue("JVM" in hello())
    }

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun kindSimpleName() {
        val kind = Int.serializer().descriptor.kind
        val name = kind.toString()
        assertEquals("INT", name)
    }

    @Serializable
    sealed interface MyInterface {
        companion object NamedCompanionObject {
            const val Example = "testing"
        }
    }

    @Serializable
    @SerialName("FooBar")
    data class FooBar(val value: String): MyInterface

    @Test
    fun testSerializersAreIntrinsified() {
        val instance = FooBar("foobar")
        // regular reflection can't get serializer for sealed interface with named companion
        assertFailsWith<SerializationException> {
            Json.encodeToString(serializer(typeOf<MyInterface>()), instance)
        }
        // But intrinsic should be able to
        assertEquals("""{"type":"FooBar","value":"foobar"}""", Json.encodeToString<MyInterface>(instance))
    }
}
