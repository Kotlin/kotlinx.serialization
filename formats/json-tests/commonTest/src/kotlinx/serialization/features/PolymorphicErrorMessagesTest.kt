/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.checkSerializationException
import kotlinx.serialization.test.isJs
import kotlinx.serialization.test.isWasm
import kotlin.test.*

class PolymorphicErrorMessagesTest : JsonTestBase() {
    @Serializable
    class DummyData(@Polymorphic val a: Any)

    @Serializable
    class Holder(val d: DummyData)

    @Test
    fun testUnknownSubclassDuringEncoding() = parametrizedTest { mode ->
        val input = Holder(DummyData(StringData("x")))
        val subclassName = if (isJs() || isWasm()) "StringData" else "kotlinx.serialization.StringData"
        checkSerializationException({
            default.encodeToString(Holder.serializer(), input, mode)
        }) { message ->
            val expected = "Serializer for subclass '$subclassName' is not found in the polymorphic scope of 'Any'.\n" +
                "Check if class with serial name '$subclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class '$subclassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
            assertEquals(expected, message)
        }
    }

    class UnregisteredData(val value: String)

    @Test
    fun testUnregisteredSubclassReportsQualifiedName() = parametrizedTest { mode ->
        val input = Holder(DummyData(UnregisteredData("x")))
        val subclassName = if (isJs() || isWasm()) {
            "UnregisteredData"
        } else {
            "kotlinx.serialization.features.PolymorphicErrorMessagesTest.UnregisteredData"
        }
        checkSerializationException({
            default.encodeToString(Holder.serializer(), input, mode)
        }) { message ->
            val expected = "Serializer for subclass '$subclassName' is not found in the polymorphic scope of 'Any'.\n" +
                "Check if class with serial name '$subclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class '$subclassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
            assertEquals(expected, message)
        }
    }

    @Test
    fun testJsonContentPolymorphicSerializerMessageContainsQualifiedName() {
        val serializer = object : JsonContentPolymorphicSerializer<Any>(Any::class) {
            override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Any> =
                throw UnsupportedOperationException()
        }
        val subclassName = if (isJs() || isWasm()) {
            "UnregisteredData"
        } else {
            "kotlinx.serialization.features.PolymorphicErrorMessagesTest.UnregisteredData"
        }
        val exception = assertFailsWith<SerializationException> {
            default.encodeToString(serializer, UnregisteredData("x"))
        }
        val expected = "Class '$subclassName' is not registered for polymorphic serialization in the scope of 'Any'.\n" +
            "Mark the base class as 'sealed' or register the serializer explicitly."
        assertEquals(expected, exception.message)
    }

    sealed class LoadingState<out T : Any> {
        data class Ready<T : Any>(val result: String) : LoadingState<T>()
    }

    @Test
    fun testLoadingStateReadyMessageContainsQualifiedName() = parametrizedTest { mode ->
        val subclassName = if (isJs() || isWasm()) {
            "Ready"
        } else {
            "kotlinx.serialization.features.PolymorphicErrorMessagesTest.LoadingState.Ready"
        }
        checkSerializationException({
            default.encodeToString<LoadingState<*>>(PolymorphicSerializer(LoadingState::class), LoadingState.Ready<String>("ok"), mode)
        }) { message ->
            val expected = "Serializer for subclass '$subclassName' is not found in the polymorphic scope of 'LoadingState'.\n" +
                "Check if class with serial name '$subclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class '$subclassName' has to be '@Serializable', and the base class 'LoadingState' has to be sealed and '@Serializable'."
            assertEquals(expected, message)
        }
    }

    @Test
    fun testLocalClassDuringEncoding() = parametrizedTest { mode ->
        class LocalData(val value: String)
        val input = Holder(DummyData(LocalData("x")))
        checkSerializationException({
            default.encodeToString(Holder.serializer(), input, mode)
        }) { message ->
            val expected = "Serializer for subclass 'LocalData' is not found in the polymorphic scope of 'Any'.\n" +
                "Check if class with serial name 'LocalData' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class 'LocalData' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
            assertEquals(expected, message)
        }
    }

    @Test
    fun testAnonymousObjectDuringEncoding() = parametrizedTest { mode ->
        val anonymous = object : Any() {}
        val input = Holder(DummyData(anonymous))
        val subclassName = anonymous::class.toString()
        checkSerializationException({
            default.encodeToString(Holder.serializer(), input, mode)
        }) { message ->
            val expected = "Serializer for subclass '$subclassName' is not found in the polymorphic scope of 'Any'.\n" +
                "Check if class with serial name '$subclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class '$subclassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
            assertEquals(expected, message)
        }
    }

    @Test
    fun testJsonContentPolymorphicSerializerWithLoadingStateReady() {
        val serializer = object : JsonContentPolymorphicSerializer<LoadingState<*>>(LoadingState::class) {
            override fun selectDeserializer(element: JsonElement): DeserializationStrategy<LoadingState<*>> =
                throw UnsupportedOperationException()
        }
        val subclassName = if (isJs() || isWasm()) {
            "Ready"
        } else {
            "kotlinx.serialization.features.PolymorphicErrorMessagesTest.LoadingState.Ready"
        }
        val exception = assertFailsWith<SerializationException> {
            default.encodeToString(serializer, LoadingState.Ready<String>("ok"))
        }
        val expected = "Class '$subclassName' is not registered for polymorphic serialization in the scope of 'LoadingState'.\n" +
            "Mark the base class as 'sealed' or register the serializer explicitly."
        assertEquals(expected, exception.message)
    }
}
