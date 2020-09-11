/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test

class SealedPolymorphismTest {

    @Serializable
    data class FooHolder(
        val someMetadata: Int,
        val payload: List<@Polymorphic Foo>
    )

    @Serializable
    @SerialName("Foo")
    sealed class Foo {
        @Serializable
        @SerialName("Bar")
        data class Bar(val bar: Int) : Foo()
        @Serializable
        @SerialName("Baz")
        data class Baz(val baz: String) : Foo()
    }

    val sealedModule = SerializersModule {
        polymorphic(Foo::class) {
            subclass(Foo.Bar.serializer())
            subclass(Foo.Baz.serializer())
        }
    }

    val json = Json { serializersModule = sealedModule }

    @Test
    fun testSaveSealedClassesList() {
        assertStringFormAndRestored(
            """{"someMetadata":42,"payload":[
            |{"type":"Bar","bar":1},
            |{"type":"Baz","baz":"2"}]}""".trimMargin().replace("\n", ""),
            FooHolder(42, listOf(Foo.Bar(1), Foo.Baz("2"))),
            FooHolder.serializer(),
            json,
            printResult = true
        )
    }

    @Test
    fun testCanSerializeSealedClassPolymorphicallyOnTopLevel() {
        assertStringFormAndRestored(
            """{"type":"Bar","bar":1}""",
            Foo.Bar(1),
            PolymorphicSerializer(Foo::class),
            json
        )
    }
}
