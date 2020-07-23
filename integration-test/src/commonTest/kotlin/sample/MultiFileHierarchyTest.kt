/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

class AbstractBaseTest {
    @Test
    fun concreteClassTest() {
        val concrete = EmptyClassA()
        val serialized: String = Json.encodeToString(EmptyClassA.serializer(), concrete)
        // to ensure that parsed without exceptions
        val parsed: EmptyClassA = Json.decodeFromString(EmptyClassA.serializer(), serialized)
    }

    @Test
    fun stubConcreteClassTest() {
        val concrete = EmptyClassB()
        val serialized: String = Json.encodeToString(EmptyClassB.serializer(), concrete)
        // to ensure that parsed without exceptions
        val parsed: EmptyClassB = Json.decodeFromString(EmptyClassB.serializer(), serialized)
    }

    @Test
    fun testCrossModuleInheritance() {
        val json = Json { allowStructuredMapKeys = true }

        val car = Car()
        car.maxSpeed = 100
        car.name = "ford"
        val s = json.encodeToString(Car.serializer(), car)
        assertEquals("""{"name":"ford","color":null,"maxSpeed":100}""", s)
        val restoredCar = json.decodeFromString(Car.serializer(), s)
        assertEquals(car, restoredCar)
    }

    @Test
    fun testCrossModuleAbstractInheritance() {
        val snippetModule = SerializersModule {
            polymorphic(Snippet::class) {
                subclass(ScreenSnippet.serializer())
                subclass(TestSnippet.serializer())
            }
        }

        val json = Json {
            serializersModule = snippetModule
        }

        val testSnippet = TestSnippet(emptyList())
        val screenSnippet = ScreenSnippet("one", "two", "three")
        val s = json.encodeToString(TestSnippet.serializer(), testSnippet)
        assertEquals(testSnippet, json.decodeFromString(TestSnippet.serializer(), s))
        assertEquals("""{"objectFieldName":"test","aaa":"aaa","experiments":[]}""",
            json.encodeToString(TestSnippet.serializer(), testSnippet)
        )
        assertStringFormAndRestored("""{"objectFieldName":"screen","aaa":"aaa","name":"one","uuid":"two","source":"three"}""",
            screenSnippet,
            ScreenSnippet.serializer(),
            json
        )
    }

    @Test
    fun testPropertiesNotInConstructor() {
        assertStringFormAndRestored("""{"b":"val b","a":"val a","c":"val c"}""", NotInConstructorTest(), NotInConstructorTest.serializer())
    }
}
