/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.*
import kotlin.jvm.*
import kotlin.test.*

@JvmInline
@Serializable
value class ComplexCarrier(val c: IntData)

@JvmInline
@Serializable
value class PrimitiveCarrier(val c: String)

data class ContextualValue(val c: String) {
    companion object: KSerializer<ContextualValue> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContextualValue", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ContextualValue) {
            encoder.encodeString(value.c)
        }

        override fun deserialize(decoder: Decoder): ContextualValue {
            return ContextualValue(decoder.decodeString())
        }
    }
}

class JsonMapKeysTest : JsonTestBase() {
    @Serializable
    private data class WithMap(val map: Map<Long, Long>)

    @Serializable
    private data class WithBooleanMap(val map: Map<Boolean, Boolean>)

    @Serializable
    private data class WithValueKeyMap(val map: Map<PrimitiveCarrier, Long>)

    @Serializable
    private data class WithEnum(val map: Map<SampleEnum, Long>)

    @Serializable
    private data class WithComplexKey(val map: Map<IntData, String>)

    @Serializable
    private data class WithComplexValueKey(val map: Map<ComplexCarrier, String>)

    @Serializable
    private data class WithContextualValueKey(val map: Map<@Contextual PrimitiveCarrier, Long>)

    @Serializable
    private data class WithContextualKey(val map: Map<@Contextual ContextualValue, Long>)

    @Test
    fun testMapKeysSupportNumbers() = parametrizedTest {
        assertStringFormAndRestored(
            """{"map":{"10":10,"20":20}}""",
            WithMap(mapOf(10L to 10L, 20L to 20L)),
            WithMap.serializer(),
            default
        )
    }

    @Test
    fun testMapKeysSupportBooleans() = parametrizedTest {
        assertStringFormAndRestored(
            """{"map":{"true":false,"false":true}}""",
            WithBooleanMap(mapOf(true to false, false to true)),
            WithBooleanMap.serializer(),
            default
        )
    }

    // As a result of quoting ignorance when parsing primitives, it is possible to parse unquoted maps if Kotlin keys are non-string primitives.
    // This is not spec-compliant, but I do not see any problems with it.
    @Test
    fun testMapDeserializesUnquotedKeys() = parametrizedTest {
        assertEquals(WithMap(mapOf(10L to 10L, 20L to 20L)), default.decodeFromString("""{"map":{10:10,20:20}}"""))
        assertEquals(
            WithBooleanMap(mapOf(true to false, false to true)),
            default.decodeFromString("""{"map":{true:false,false:true}}""")
        )
        assertFailsWithSerial("JsonDecodingException") {
            default.decodeFromString(
                MapSerializer(
                    String.serializer(),
                    Boolean.serializer()
                ),"""{"map":{true:false,false:true}}"""
            )
        }
    }

    @Test
    fun testStructuredMapKeysShouldBeProhibitedByDefault() = parametrizedTest { streaming ->
        verifyProhibition(WithComplexKey(mapOf(IntData(42) to "42")), streaming)
        verifyProhibition(WithComplexValueKey(mapOf(ComplexCarrier(IntData(42)) to "42")), streaming)
    }

    private inline fun <reified T: Any> verifyProhibition(value: T, streaming: JsonTestingMode) {
        assertFailsWithSerialMessage("JsonEncodingException", "can't be used in JSON as a key in the map") {
            Json.encodeToString(value, streaming)
        }
    }

    @Test
    fun testStructuredMapKeysAllowedWithFlag() = assertJsonFormAndRestored(
        WithComplexKey.serializer(),
        WithComplexKey(mapOf(IntData(42) to "42")),
        """{"map":[{"intV":42},"42"]}""",
        Json { allowStructuredMapKeys = true }
    )

    @Test
    fun testStructuredValueMapKeysAllowedWithFlag() {
        assertJsonFormAndRestored(
            WithComplexValueKey.serializer(),
            WithComplexValueKey(mapOf(ComplexCarrier(IntData(42)) to "42")),
            """{"map":[{"intV":42},"42"]}""",
            Json { allowStructuredMapKeys = true }
        )
    }

    @Test
    fun testEnumsAreAllowedAsMapKeys() = assertJsonFormAndRestored(
        WithEnum.serializer(),
        WithEnum(mapOf(SampleEnum.OptionA to 1L, SampleEnum.OptionC to 3L)),
        """{"map":{"OptionA":1,"OptionC":3}}""",
        Json
    )

    @Test
    fun testPrimitivesAreAllowedAsValueMapKeys() {
        assertJsonFormAndRestored(
            WithValueKeyMap.serializer(),
            WithValueKeyMap(mapOf(PrimitiveCarrier("fooKey") to 1)),
            """{"map":{"fooKey":1}}""",
            Json
        )
    }

    @Test
    fun testContextualValuePrimitivesAreAllowedAsValueMapKeys() {
        assertJsonFormAndRestored(
            WithContextualValueKey.serializer(),
            WithContextualValueKey(mapOf(PrimitiveCarrier("fooKey") to 1)),
            """{"map":{"fooKey":1}}""",
            Json {
                serializersModule =
                    SerializersModule { contextual(PrimitiveCarrier::class, PrimitiveCarrier.serializer()) }
            }
        )
    }

    @Test
    fun testContextualPrimitivesAreAllowedAsValueMapKeys() {
        assertJsonFormAndRestored(
            WithContextualKey.serializer(),
            WithContextualKey(mapOf(ContextualValue("fooKey") to 1)),
            """{"map":{"fooKey":1}}""",
            Json {
                serializersModule = SerializersModule { contextual(ContextualValue::class, ContextualValue) }
            }
        )
    }
}
