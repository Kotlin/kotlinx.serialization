/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
sealed class SealedProtocol {
    @Serializable
    data class StringMessage(val description: String, val message: String) : SealedProtocol()

    @Serializable
    data class IntMessage(val description: String, val message: Int) : SealedProtocol()

    @Serializable
    data class ErrorMessage(val error: String) : SealedProtocol()

    @Suppress("PLUGIN_ERROR") // todo: remove this suppresses when objects serialization will be merged
    @SerialName("EOF")
    @Serializable
    object EOF : SealedProtocol()
}

@Serializable
sealed class ProtocolWithAbstractClass {

    @Serializable
    abstract class Message : ProtocolWithAbstractClass() {
        @Serializable
        data class StringMessage(val description: String, val message: String) : Message()

        @Serializable
        data class IntMessage(val description: String, val message: Int) : Message()
    }

    @Serializable
    data class ErrorMessage(val error: String) : ProtocolWithAbstractClass()

    @Suppress("PLUGIN_ERROR")
    @SerialName("EOF")
    @Serializable
    object EOF : ProtocolWithAbstractClass()
}

@Serializable
sealed class ProtocolWithSealedClass {

    @Serializable
    sealed class Message : ProtocolWithSealedClass() {
        @Serializable
        data class StringMessage(val description: String, val message: String) : Message()

        @Serializable
        data class IntMessage(val description: String, val message: Int) : Message()
    }

    @Serializable
    data class ErrorMessage(val error: String) : ProtocolWithSealedClass()

    @Suppress("PLUGIN_ERROR")
    @SerialName("EOF")
    @Serializable
    object EOF : ProtocolWithSealedClass()
}

@Serializable
sealed class ProtocolWithGenericClass {

    @Serializable
    data class Message<T>(val description: String, val message: T) : ProtocolWithGenericClass()

    @Serializable
    data class ErrorMessage(val error: String) : ProtocolWithGenericClass()

    @Suppress("PLUGIN_ERROR")
    @SerialName("EOF")
    @Serializable
    object EOF : ProtocolWithGenericClass()
}

val ManualSerializer = SealedClassSerializer(
    "SimpleSealed",
    SimpleSealed::class,
    arrayOf(SimpleSealed.SubSealedA::class, SimpleSealed.SubSealedB::class),
    arrayOf(SimpleSealed.SubSealedA.serializer(), SimpleSealed.SubSealedB.serializer())
)

@Serializable
data class SealedHolder(val s: SimpleSealed)

@Serializable
data class SealedBoxHolder(val b: Box<SimpleSealed>)

class SealedClassesSerializationTest {
    private val arrayJson = Json(JsonConfiguration.Default.copy(useArrayPolymorphism = true))
    private val json = Json(JsonConfiguration.Default.copy(useArrayPolymorphism = false, prettyPrint = false))

    @Test
    fun manualSerializer() {
        val message = json.stringify(
            ManualSerializer,
            SimpleSealed.SubSealedB(42)
        )
        assertEquals("{\"type\":\"kotlinx.serialization.SimpleSealed.SubSealedB\",\"i\":42}", message)
    }

    @Test
    fun onTopLevel() {
        val arrayMessage = arrayJson.stringify(
            SimpleSealed.serializer(),
            SimpleSealed.SubSealedB(42)
        )
        val message = json.stringify(
            SimpleSealed.serializer(),
            SimpleSealed.SubSealedB(42)
        )
        assertEquals("{\"type\":\"kotlinx.serialization.SimpleSealed.SubSealedB\",\"i\":42}", message)
        assertEquals("[\"kotlinx.serialization.SimpleSealed.SubSealedB\",{\"i\":42}]", arrayMessage)
    }

    @Test
    fun insideClass() {
        assertStringFormAndRestored(
            """{"s":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}""",
            SealedHolder(SimpleSealed.SubSealedA("foo")),
            SealedHolder.serializer(),
            json
        )
    }

    @Test
    fun insideGeneric() {
        assertStringFormAndRestored(
            """{"boxed":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}""",
            Box<SimpleSealed>(SimpleSealed.SubSealedA("foo")),
            Box.serializer(SimpleSealed.serializer()),
            json
        )
        assertStringFormAndRestored(
            """{"b":{"boxed":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}}""",
            SealedBoxHolder(Box(SimpleSealed.SubSealedA("foo"))),
            SealedBoxHolder.serializer(),
            json
        )
    }

    @Test
    fun complexProtocol() {
        val messages = listOf<SealedProtocol>(
            SealedProtocol.StringMessage("string message", "foo"),
            SealedProtocol.IntMessage("int message", 42),
            SealedProtocol.ErrorMessage("requesting termination"),
            SealedProtocol.EOF
        )
        val expected =
            """[{"type":"kotlinx.serialization.features.SealedProtocol.StringMessage","description":"string message","message":"foo"},{"type":"kotlinx.serialization.features.SealedProtocol.IntMessage","description":"int message","message":42},{"type":"kotlinx.serialization.features.SealedProtocol.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertStringFormAndRestored(expected, messages, SealedProtocol.serializer().list, json)
    }

    @Test
    fun protocolWithAbstractClass() {
        val messages = listOf<ProtocolWithAbstractClass>(
            ProtocolWithAbstractClass.Message.StringMessage("string message", "foo"),
            ProtocolWithAbstractClass.Message.IntMessage("int message", 42),
            ProtocolWithAbstractClass.ErrorMessage("requesting termination"),
            ProtocolWithAbstractClass.EOF
        )
        val abstractContext = SerializersModule {
            polymorphic(ProtocolWithAbstractClass::class, ProtocolWithAbstractClass.Message::class) {
                ProtocolWithAbstractClass.Message.IntMessage::class with ProtocolWithAbstractClass.Message.IntMessage.serializer()
                ProtocolWithAbstractClass.Message.StringMessage::class with ProtocolWithAbstractClass.Message.StringMessage.serializer()
            }
        }
        val json =
            Json(
                JsonConfiguration.Default.copy(useArrayPolymorphism = false, prettyPrint = false),
                context = abstractContext
            )
        val expected =
            """[{"type":"kotlinx.serialization.features.ProtocolWithAbstractClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"kotlinx.serialization.features.ProtocolWithAbstractClass.Message.IntMessage","description":"int message","message":42},{"type":"kotlinx.serialization.features.ProtocolWithAbstractClass.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertStringFormAndRestored(expected, messages, ProtocolWithAbstractClass.serializer().list, json)
    }

    @Test
    fun protocolWithSealedClass() {
        val messages = listOf<ProtocolWithSealedClass>(
            ProtocolWithSealedClass.Message.StringMessage("string message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int message", 42),
            ProtocolWithSealedClass.ErrorMessage("requesting termination"),
            ProtocolWithSealedClass.EOF
        )
        val expected =
            """[{"type":"kotlinx.serialization.features.ProtocolWithSealedClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"kotlinx.serialization.features.ProtocolWithSealedClass.Message.IntMessage","description":"int message","message":42},{"type":"kotlinx.serialization.features.ProtocolWithSealedClass.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertStringFormAndRestored(expected, messages, ProtocolWithSealedClass.serializer().list, json)
    }

    @Test
    fun partOfProtocolWithSealedClass() {
        val messages = listOf<ProtocolWithSealedClass.Message>(
            ProtocolWithSealedClass.Message.StringMessage("string message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int message", 42)
        )
        val expected =
            """[{"type":"kotlinx.serialization.features.ProtocolWithSealedClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"kotlinx.serialization.features.ProtocolWithSealedClass.Message.IntMessage","description":"int message","message":42}]"""

        assertStringFormAndRestored(expected, messages, ProtocolWithSealedClass.serializer().list, json)
        assertStringFormAndRestored(expected, messages, ProtocolWithSealedClass.Message.serializer().list, json)
    }

    @Test
    fun protocolWithGenericClass() {
        val messages = listOf<ProtocolWithGenericClass>(
            ProtocolWithGenericClass.Message<String>("string message", "foo"),
            ProtocolWithGenericClass.Message<Int>("int message", 42),
            ProtocolWithGenericClass.ErrorMessage("requesting termination"),
            ProtocolWithGenericClass.EOF
        )
        val expected =
            """[["kotlinx.serialization.features.ProtocolWithGenericClass.Message",{"description":"string message","message":["kotlin.String","foo"]}],["kotlinx.serialization.features.ProtocolWithGenericClass.Message",{"description":"int message","message":["kotlin.Int",42]}],["kotlinx.serialization.features.ProtocolWithGenericClass.ErrorMessage",{"error":"requesting termination"}],["EOF",{}]]"""
        val json = Json(JsonConfiguration.Default.copy(useArrayPolymorphism = true))
        assertStringFormAndRestored(expected, messages, ProtocolWithGenericClass.serializer().list, json)
    }
}
