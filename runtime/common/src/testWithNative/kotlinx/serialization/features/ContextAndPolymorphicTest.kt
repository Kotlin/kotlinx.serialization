/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
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
        @ContextualSerialization val stringPayload: Payload,
        @Serializable(with = BinaryPayloadSerializer::class) val binaryPayload: Payload
    )

    @Serializable
    data class Payload(val s: String)

    @Serializable
    data class PayloadList(val ps: List<@ContextualSerialization Payload>)

    @Serializer(forClass = Payload::class)
    object PayloadSerializer {}

    object BinaryPayloadSerializer : KSerializer<Payload> {
        override val descriptor: SerialDescriptor = SerialClassDescImpl("Payload")

        override fun serialize(encoder: Encoder, obj: Payload) {
            encoder.encodeString(HexConverter.printHexBinary(obj.s.toUtf8Bytes()))
        }

        override fun deserialize(decoder: Decoder): Payload {
            return Payload(stringFromUtf8Bytes(HexConverter.parseHexBinary(decoder.decodeString())))
        }
    }

    private val obj = EnhancedData(Data(100500), Payload("string"), Payload("binary"))
    private lateinit var json: Json

    @BeforeTest
    fun initContext() {
        val scope = serializersModuleOf(Payload::class, PayloadSerializer)
        val bPolymorphicModule = SerializersModule { polymorphic(Any::class) { Payload::class with PayloadSerializer } }
        json = Json(
            JsonConfiguration(unquoted = true, useArrayPolymorphism = true),
            context = scope + bPolymorphicModule
        )
    }

    @Test
    fun testWriteCustom() {
        val s = json.stringify(EnhancedData.serializer(), obj)
        assertEquals("{data:{a:100500,b:42},stringPayload:{s:string},binaryPayload:62696E617279}", s)
    }

    @Test
    fun testReadCustom() {
        val s = json.parse(EnhancedData.serializer(), "{data:{a:100500,b:42},stringPayload:{s:string},binaryPayload:62696E617279}")
        assertEquals(obj, s)
    }

    @Test
    fun testWriteCustomList() {
        val s = json.stringify(PayloadList.serializer(), PayloadList(listOf(Payload("1"), Payload("2"))))
        assertEquals("{ps:[{s:1},{s:2}]}", s)
    }

    @Test
    fun testPolymorphicResolve() {
        val map = mapOf<String, Any>("Payload" to Payload("data"))
        val serializer = (StringSerializer to PolymorphicSerializer(Any::class)).map
        val s = json.stringify(serializer, map)
        assertEquals("""{Payload:[kotlinx.serialization.features.ContextAndPolymorphicTest.Payload,{s:data}]}""", s)
    }

    @Test
    fun testDifferentRepresentations() {
        val simpleModule = serializersModule(PayloadSerializer)
        // MapModule and CompositeModule are also available
        val binaryModule = serializersModule(BinaryPayloadSerializer)

        val json1 = Json { useArrayPolymorphism = true; serialModule = simpleModule }
        val json2 = Json { useArrayPolymorphism = true; serialModule = binaryModule }

        // in json1, Payload would be serialized with PayloadSerializer,
        // in json2, Payload would be serialized with BinaryPayloadSerializer

        val list = PayloadList(listOf(Payload("string")))
        assertEquals("""{"ps":[{"s":"string"}]}""", json1.stringify(PayloadList.serializer(), list))
        assertEquals("""{"ps":["737472696E67"]}""", json2.stringify(PayloadList.serializer(), list))
    }
}
