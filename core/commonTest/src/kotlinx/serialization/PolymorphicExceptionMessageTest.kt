/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.isJs
import kotlinx.serialization.test.isJvm
import kotlinx.serialization.test.isWasm
import kotlin.test.*

class TopLevelUnregisteredClass

class PolymorphicExceptionMessageTest {
    sealed class LoadingState<out T : Any> {
        data class Ready<T : Any>(val result: String) : LoadingState<T>()
    }

    class UnregisteredSubclass

    @Test
    fun testIssue3268ReproducerLoadingStateReady() {
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(LoadingState.Ready::class, LoadingState::class)
        }
        val expectedSubclassName = if (isJs() || isWasm()) {
            "Ready"
        } else {
            "kotlinx.serialization.PolymorphicExceptionMessageTest.LoadingState.Ready"
        }
        val expectedMessage = "Serializer for subclass '$expectedSubclassName' is not found in the polymorphic scope of 'LoadingState'.\n" +
            "Check if class with serial name '$expectedSubclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$expectedSubclassName' has to be '@Serializable', and the base class 'LoadingState' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testUnregisteredSubclassReportsQualifiedName() {
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(UnregisteredSubclass::class, Any::class)
        }
        val expectedSubclassName = if (isJs() || isWasm()) {
            "UnregisteredSubclass"
        } else {
            "kotlinx.serialization.PolymorphicExceptionMessageTest.UnregisteredSubclass"
        }
        val expectedMessage = "Serializer for subclass '$expectedSubclassName' is not found in the polymorphic scope of 'Any'.\n" +
            "Check if class with serial name '$expectedSubclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$expectedSubclassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testTopLevelUnregisteredClassReportsQualifiedName() {
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(TopLevelUnregisteredClass::class, Any::class)
        }
        val expectedSubclassName = if (isJs() || isWasm()) {
            "TopLevelUnregisteredClass"
        } else {
            "kotlinx.serialization.TopLevelUnregisteredClass"
        }
        val expectedMessage = "Serializer for subclass '$expectedSubclassName' is not found in the polymorphic scope of 'Any'.\n" +
            "Check if class with serial name '$expectedSubclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$expectedSubclassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testLocalClassFallsBackToSimpleName() {
        class LocalSubclass
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(LocalSubclass::class, Any::class)
        }
        val expectedMessage = "Serializer for subclass 'LocalSubclass' is not found in the polymorphic scope of 'Any'.\n" +
            "Check if class with serial name 'LocalSubclass' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class 'LocalSubclass' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testAnonymousObjectFallback() {
        val anonymousObj = object : Any() {}
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(anonymousObj::class, Any::class)
        }
        val expectedClassName = anonymousObj::class.toString()
        val expectedMessage = "Serializer for subclass '$expectedClassName' is not found in the polymorphic scope of 'Any'.\n" +
            "Check if class with serial name '$expectedClassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$expectedClassName' has to be '@Serializable', and the base class 'Any' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
        if (isJvm()) {
            assertTrue(expectedClassName.contains("$"))
        }
    }

    private object DummyEncoder : AbstractEncoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
    }

    @Test
    fun testPolymorphicSerializerEndToEndMessage() {
        val serializer = PolymorphicSerializer(LoadingState::class)
        val exception = assertFailsWith<SerializationException> {
            serializer.serialize(DummyEncoder, LoadingState.Ready<String>("ok"))
        }
        val expectedSubclassName = if (isJs() || isWasm()) {
            "Ready"
        } else {
            "kotlinx.serialization.PolymorphicExceptionMessageTest.LoadingState.Ready"
        }
        val expectedMessage = "Serializer for subclass '$expectedSubclassName' is not found in the polymorphic scope of 'LoadingState'.\n" +
            "Check if class with serial name '$expectedSubclassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$expectedSubclassName' has to be '@Serializable', and the base class 'LoadingState' has to be sealed and '@Serializable'."
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testMissingDiscriminatorMessage() {
        val exception = assertFailsWith<SerializationException> {
            throwSubtypeNotRegistered(null, LoadingState::class)
        }
        val expectedMessage = "Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'LoadingState'."
        assertEquals(expectedMessage, exception.message)
    }
}
