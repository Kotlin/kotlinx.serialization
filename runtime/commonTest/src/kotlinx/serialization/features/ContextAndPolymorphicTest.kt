/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class ContextAndPolymorphicTest {

    @Serializable
    data class Data(val a: Int, val b: Int = 42)

    @Serializable
    data class EnhancedData(
        val data: Data,
        @Contextual val stringPayload: Payload,
        @Serializable(with = BinaryPayloadSerializer::class) val binaryPayload: Payload
    )

    @Serializable
    @SerialName("Payload")
    data class Payload(val s: String)

    @Serializable
    data class PayloadList(val ps: List<@Contextual Payload>)

    @Serializer(forClass = Payload::class)
    object PayloadSerializer

    object BinaryPayloadSerializer : KSerializer<Payload> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BinaryPayload", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Payload) {
            encoder.encodeString(InternalHexConverter.printHexBinary(value.s.encodeToByteArray()))
        }

        override fun deserialize(decoder: Decoder): Payload {
            return Payload(InternalHexConverter.parseHexBinary(decoder.decodeString()).decodeToString())
        }
    }

    private val value = EnhancedData(Data(100500), Payload("string"), Payload("binary"))
    private lateinit var json: Json

    @BeforeTest
    fun initContext() {
        val scope = serializersModuleOf(Payload::class, PayloadSerializer)
        val bPolymorphicModule = SerializersModule { polymorphic(Any::class) { subclass(PayloadSerializer) } }
        json = Json {
            useArrayPolymorphism = true
            serializersModule = scope + bPolymorphicModule
        }
    }

    @Test
    fun testWriteCustom() {
        val s = json.encodeToString(EnhancedData.serializer(), value)
        assertEquals("""{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696E617279"}""", s)
    }

    @Test
    fun testReadCustom() {
        val s = json.decodeFromString(EnhancedData.serializer(),
            """{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696E617279"}""")
        assertEquals(value, s)
    }

    @Test
    fun testWriteCustomList() {
        val s = json.encodeToString(PayloadList.serializer(), PayloadList(listOf(Payload("1"), Payload("2"))))
        assertEquals("""{"ps":[{"s":"1"},{"s":"2"}]}""", s)
    }

    @Test
    fun testPolymorphicResolve() {
        val map = mapOf<String, Any>("Payload" to Payload("data"))
        val serializer = MapSerializer(String.serializer(), PolymorphicSerializer(Any::class))
        val s = json.encodeToString(serializer, map)
        assertEquals("""{"Payload":["Payload",{"s":"data"}]}""", s)
    }

    @Test
    fun testDifferentRepresentations() {
        val simpleModule = serializersModuleOf(PayloadSerializer)
        val binaryModule = serializersModuleOf(BinaryPayloadSerializer)

        val json1 = Json { useArrayPolymorphism = true; serializersModule = simpleModule }
        val json2 = Json { useArrayPolymorphism = true; serializersModule = binaryModule }

        // in json1, Payload would be serialized with PayloadSerializer,
        // in json2, Payload would be serialized with BinaryPayloadSerializer

        val list = PayloadList(listOf(Payload("string")))
        assertEquals("""{"ps":[{"s":"string"}]}""", json1.encodeToString(PayloadList.serializer(), list))
        assertEquals("""{"ps":["737472696E67"]}""", json2.encodeToString(PayloadList.serializer(), list))
    }

    private fun SerialDescriptor.inContext(module: SerializersModule): SerialDescriptor = when (kind) {
        SerialKind.CONTEXTUAL -> requireNotNull(module.getContextualDescriptor(this)) { "Expected $this to be registered in module" }
        else -> error("Expected this function to be called on CONTEXTUAL descriptor")
    }

    @Test
    fun testResolveContextualDescriptor() {
        val simpleModule = serializersModuleOf(PayloadSerializer)
        val binaryModule = serializersModuleOf(BinaryPayloadSerializer)

        val contextDesc = EnhancedData.serializer().descriptor.elementDescriptors.toList()[1] // @ContextualSer stringPayload
        assertEquals(SerialKind.CONTEXTUAL, contextDesc.kind)
        assertEquals(0, contextDesc.elementsCount)

        val resolvedToDefault = contextDesc.inContext(simpleModule)
        assertEquals(StructureKind.CLASS, resolvedToDefault.kind)
        assertEquals("Payload", resolvedToDefault.serialName)
        assertEquals(1, resolvedToDefault.elementsCount)

        val resolvedToBinary = contextDesc.inContext(binaryModule)
        assertEquals(PrimitiveKind.STRING, resolvedToBinary.kind)
        assertEquals("BinaryPayload", resolvedToBinary.serialName)
    }

    @Test
    fun testContextualSerializerUsesDefaultIfModuleIsEmpty() {
        val s = Json { useArrayPolymorphism = true }.encodeToString(EnhancedData.serializer(), value)
        assertEquals("""{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696E617279"}""", s)
    }
}
