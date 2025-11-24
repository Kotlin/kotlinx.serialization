/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class PolymorphicSealedChildTest {

    @Serializable
    data class FooHolder(
        val someMetadata: Int,
        val payload: List<FooBase>
    )

    @Serializable
    abstract class FooBase

    @Serializable
    @SerialName("Foo")
    sealed class Foo: FooBase() {
        @Serializable
        @SerialName("Bar")
        data class Bar(val bar: Int) : Foo()
        @Serializable
        @SerialName("Baz")
        sealed class Baz : Foo()

        @Serializable
        @SerialName("BazC1")
        data class SealedBazChild1(val baz: String): Baz()
        @Serializable
        @SerialName("BazC2")
        data class SealedBazChild2(val baz: String): Baz()

    }

    @Serializable
    sealed class FooOpen: FooBase() {
        @Serializable
        data class Bar(val bar: Int) : FooOpen()
        @Serializable
        abstract class Baz: FooOpen()
        @Serializable
        @SerialName("BazOC1")
        data class BazChild1(val baz: String) : Baz()
        @Serializable
        @SerialName("BazOC2")
        data class BazChild2(val baz: String) : Baz()
    }

    val sealedModule = SerializersModule {
        polymorphic(FooBase::class) {
            subclassesOfSealed<Foo>()
        }
    }

    val json = Json { serializersModule = sealedModule }

    /**
     * Test that attempting to register a grandchild (any descendant of the sealed intermediary)
     * fails with an exception
     */
    @Test
    fun testOpenGrandchildIsInvalid() {

        lateinit var openJson: Json
        val e = assertFailsWith<IllegalArgumentException> {
            openJson = Json {
                serializersModule = SerializersModule {
                    polymorphic(FooBase::class) {
                        subclassesOfSealed<FooOpen>()
                        fail("This code should be unreachable as the previous operation fails")
                        subclass(FooOpen.BazChild1::class)
                        subclass(FooOpen.BazChild2::class)
                    }
                }
            }

            fail("Unreachable code that would represent the usage if valid (which is isn't per policy)")
            assertStringFormAndRestored(
                """{"someMetadata":43,"payload":[{"type":"BazOC1","baz":"aaa"}]}""",
                FooHolder(43,listOf(FooOpen.BazChild1("aaa"))),
                FooHolder.serializer(),
                openJson
            )
        }
        assertContains(assertNotNull(e.message), "FooOpen", )
        assertContains(assertNotNull(e.message), "incomplete hierarchy", )
    }

    /**
     * This tests both a directly sealed child (Bar) and a (sealed) grandchild (SealedBazChild1)
     * Nesting sealed hierarchies is valid (and flattened by the plugin).
     */
    @Test
    fun testSaveSealedClassesList() {
        assertStringFormAndRestored(
            """{"someMetadata":42,"payload":[
            |{"type":"Bar","bar":1},
            |{"type":"BazC1","baz":"2"}]}""".trimMargin().replace("\n", ""),
            FooHolder(42, listOf(Foo.Bar(1), Foo.SealedBazChild1("2"))),
            FooHolder.serializer(),
            json,
        )
    }

    /**
     * Test that a simple direct descendant of the serialized base type can be serialized.
     */
    @Test
    fun testCanSerializeSealedClassPolymorphicallyOnTopLevel() {
        assertStringFormAndRestored(
            """{"type":"Bar","bar":1}""",
            Foo.Bar(1),
            PolymorphicSerializer(FooBase::class),
            json
        )
    }
}
