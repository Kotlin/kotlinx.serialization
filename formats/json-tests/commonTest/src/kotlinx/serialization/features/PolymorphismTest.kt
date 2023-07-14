/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class PolymorphismTest : JsonTestBase() {

    @Serializable
    data class Wrapper(
        @Id(1) @Polymorphic val polyBase1: PolyBase,
        @Id(2) @Polymorphic val polyBase2: PolyBase
    )

    private val module: SerializersModule = BaseAndDerivedModule + SerializersModule {
        polymorphic(
            PolyDerived::class,
            PolyDerived.serializer()
        )
    }

    private val json = Json { useArrayPolymorphism = true; serializersModule = module }

    @Test
    fun testInheritanceJson() = parametrizedTest { jsonTestingMode ->
        val obj = Wrapper(
            PolyBase(2),
            PolyDerived("b")
        )
        val bytes = json.encodeToString(Wrapper.serializer(), obj, jsonTestingMode)
        assertEquals(
            """{"polyBase1":["kotlinx.serialization.PolyBase",{"id":2}],""" +
                    """"polyBase2":["kotlinx.serialization.PolyDerived",{"id":1,"s":"b"}]}""", bytes
        )
    }

    @Test
    fun testSerializeWithExplicitPolymorphicSerializer() = parametrizedTest { jsonTestingMode ->
        val obj = PolyDerived("b")
        val s = json.encodeToString(PolymorphicSerializer(PolyDerived::class), obj, jsonTestingMode)
        assertEquals("""["kotlinx.serialization.PolyDerived",{"id":1,"s":"b"}]""", s)
    }

    object PolyDefaultDeserializer : JsonTransformingSerializer<PolyDefault>(PolyDefault.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
            put("json", JsonObject(element.jsonObject.filterKeys { it != "type" }))
            put("id", 42)
        }
    }

    object EvenDefaultSerializer : SerializationStrategy<PolyBase> {
        override val descriptor = buildClassSerialDescriptor("even") {
            element<String>("parity")
        }

        override fun serialize(encoder: Encoder, value: PolyBase) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "even")
            }
        }
    }

    object OddDefaultSerializer : SerializationStrategy<PolyBase> {
        override val descriptor = buildClassSerialDescriptor("odd") {
            element<String>("parity")
        }

        override fun serialize(encoder: Encoder, value: PolyBase) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "odd")
            }
        }
    }

    @Test
    fun testDefaultDeserializer() = parametrizedTest { jsonTestingMode ->
        val withDefault = module + SerializersModule {
            polymorphicDefaultDeserializer(PolyBase::class) { name ->
                if (name == "foo") {
                    PolyDefaultDeserializer
                } else {
                    null
                }
            }
        }

        val adjustedJson = Json { serializersModule = withDefault }
        val string = """
            {"polyBase1":{"type":"kotlinx.serialization.PolyBase","id":239},
            "polyBase2":{"type":"foo","key":42}}""".trimIndent()
        val result = adjustedJson.decodeFromString(Wrapper.serializer(), string, jsonTestingMode)
        assertEquals(Wrapper(PolyBase(239), PolyDefault(JsonObject(mapOf("key" to JsonPrimitive(42))))), result)

        val replaced = string.replace("foo", "bar")
        assertFailsWithMessage<SerializationException>("not found") { adjustedJson.decodeFromString(Wrapper.serializer(), replaced, jsonTestingMode) }
    }

    @Test
    fun testDefaultDeserializerForMissingDiscriminator() = parametrizedTest { jsonTestingMode ->
        val json = Json {
            serializersModule = module + SerializersModule {
                polymorphicDefaultDeserializer(PolyBase::class) { name ->
                    if (name == null) {
                        PolyDefaultDeserializer
                    } else {
                        null
                    }
                }
            }
        }
        val string = """
            {"polyBase1":{"type":"kotlinx.serialization.PolyBase","id":239},
            "polyBase2":{"key":42}}""".trimIndent()
        val result = json.decodeFromString(Wrapper.serializer(), string, jsonTestingMode)
        assertEquals(Wrapper(PolyBase(239), PolyDefault(JsonObject(mapOf("key" to JsonPrimitive(42))))), result)
    }

    @Test
    fun testDefaultSerializer() = parametrizedTest { jsonTestingMode ->
        val json = Json {
            serializersModule = module + SerializersModule {
                polymorphicDefaultSerializer(PolyBase::class) { value ->
                    if (value.id % 2 == 0) {
                        EvenDefaultSerializer
                    } else {
                        OddDefaultSerializer
                    }
                }
            }
        }
        val obj = Wrapper(
            PolyDefaultWithId(0),
            PolyDefaultWithId(1)
        )
        val s = json.encodeToString(Wrapper.serializer(), obj, jsonTestingMode)
        assertEquals("""{"polyBase1":{"type":"even","parity":"even"},"polyBase2":{"type":"odd","parity":"odd"}}""", s)
    }

    @Serializable
    sealed class Conf {
        @Serializable
        @SerialName("empty")
        object Empty : Conf() // default

        @Serializable
        @SerialName("simple")
        data class Simple(val value: String) : Conf()
    }

    private val jsonForConf = Json {
        isLenient = false
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphicDefaultDeserializer(Conf::class) { Conf.Empty.serializer() }
        }
    }

    @Test
    fun defaultSerializerWithEmptyBodyTest() = parametrizedTest { mode ->
        assertEquals(Conf.Simple("123"), jsonForConf.decodeFromString<Conf>("""{"type": "simple", "value": "123"}""", mode))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{"type": "default"}""", mode))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{"unknown": "Meow"}""", mode))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{}""", mode))
    }

    @Test
    fun testTypeKeysInLenientMode() = parametrizedTest { mode ->
        val json = Json(jsonForConf) { isLenient = true }

        assertEquals(Conf.Simple("123"), json.decodeFromString<Conf>("""{type: simple, value: 123}""", mode))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{type: default}""", mode))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{unknown: Meow}""", mode))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{}""", mode))

    }
}
