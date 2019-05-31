/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test

@Serializable
data class FooHolder(
    val someMetadata: Int,
    val payload: List<@Polymorphic Foo>
)

@Serializable
sealed class Foo {
    @Serializable
    data class Bar(val bar: Int) : Foo()
    @Serializable
    data class Baz(val baz: String) : Foo()
}

class SealedPolymorphismTest {

    val sealedModule = SerializersModule {
        polymorphic(Foo::class) {
            Foo.Bar::class with Foo.Bar.serializer()
            Foo.Baz::class with Foo.Baz.serializer()
        }
    }

    val json = Json(context = sealedModule)

    @Test
    fun testSaveSealedClassesList() {
        assertStringFormAndRestored(
            """{"someMetadata":42,"payload":[
            |{"type":"kotlinx.serialization.features.Foo.Bar","bar":1},
            |{"type":"kotlinx.serialization.features.Foo.Baz","baz":"2"}]}""".trimMargin().replace("\n", ""),
            FooHolder(42, listOf(Foo.Bar(1), Foo.Baz("2"))),
            FooHolder.serializer(),
            json,
            printResult = true
        )
    }

    @Test
    fun testCanSerializeSealedClassPolymorphicallyOnTopLevel() {
        assertStringFormAndRestored(
            """{"type":"kotlinx.serialization.features.Foo.Bar","bar":1}""",
            Foo.Bar(1),
            PolymorphicSerializer(Foo::class),
            json
        )
    }
}
