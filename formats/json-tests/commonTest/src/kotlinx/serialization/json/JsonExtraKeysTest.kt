/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.test.checkSerializationException
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JsonExtraKeysTest : JsonTestBase() {

    @Serializable
    data class Basic(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    data class NonOptionalBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement>
    )

    @Serializable
    data class WithNullable(
        val a: Int,
        val b: String? = null,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    data class WithAlias(
        val a: Int,
        @JsonNames("b_alias") val b: String,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    data class WithNaming(
        val someValue: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    @JsonIgnoreUnknownKeys
    data class IgnoresUnknown(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    sealed class Base

    @Serializable
    @SerialName("derived")
    data class Derived(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    ) : Base()

    @Serializable
    data class Wrapper(val base: Base)

    @Serializable
    data class BadBucket(
        val a: Int,
        @JsonNames("extras_alias")
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    data class TwoSiblings(val first: Basic, val second: Basic)

    @Serializable
    data class RecursiveNode(
        val value: Int,
        val child: RecursiveNode? = null,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    data class OuterWithNestedPoly(
        val base: Base,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Test
    fun testBasicCapture() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"a":1,"b":"text","c":[1,2],"d":{"x":true},"e":null}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive("text"), result.extras["b"])
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))), result.extras["c"])
        assertEquals(JsonObject(mapOf("x" to JsonPrimitive(true))), result.extras["d"])
        assertEquals(JsonNull, result.extras["e"])

        val roundTrip = json.encodeToString(result, mode)
        val result2 = json.decodeFromString<Basic>(roundTrip, mode)
        assertEquals(result, result2)
    }

    @Test
    fun testCaptureWinsOverIgnoreUnknownKeys() = parametrizedTest { mode ->
        val json = Json(default) { ignoreUnknownKeys = true; encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testCaptureWinsOverAnnotation() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<IgnoresUnknown>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testJsonNamesInteraction() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"a":1,"b_alias":"value","unknown":42}"""
        val result = json.decodeFromString<WithAlias>(input, mode)
        assertEquals(1, result.a)
        assertEquals("value", result.b)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testNamingStrategy() = parametrizedTest { mode ->
        val json = Json(default) { namingStrategy = JsonNamingStrategy.SnakeCase; encodeDefaults = true }
        val input = """{"some_value":1,"some_other":42}"""
        val result = json.decodeFromString<WithNaming>(input, mode)
        assertEquals(1, result.someValue)
        assertEquals(JsonPrimitive(42), result.extras["some_other"])
    }

    @Test
    fun testRejectJsonNamesOnBucket() {
        val json = Json(default) { encodeDefaults = true }
        assertFailsWith<SerializationException> {
            json.decodeFromString<BadBucket>("""{"a":1}""")
        }
    }

    @Test
    fun testExplicitNullsFalse() = parametrizedTest { mode ->
        val json = Json(default) { explicitNulls = false; encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<WithNullable>(input, mode)
        assertEquals(1, result.a)
        assertNull(result.b)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testDiscriminatorExcluded() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"base":{"type":"derived","a":1,"unknown":42}}"""
        val result = json.decodeFromString<Wrapper>(input, mode)
        val derived = result.base as Derived
        assertEquals(1, derived.a)
        assertEquals(JsonPrimitive(42), derived.extras["unknown"])
        assertNull(derived.extras["type"])
    }

    @Test
    fun testBucketOwnNameCaptured() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"a":1,"extras":42}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["extras"])
    }

    @Test
    fun testEncodeCollisionDeclaredName() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val data = Basic(a = 1, extras = mapOf("a" to JsonPrimitive(2)))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testEncodeCollisionDiscriminator() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val data: Base = Derived(a = 1, extras = mapOf("type" to JsonPrimitive("foo")))
        checkSerializationException({
            json.encodeToString(Base.serializer(), data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with the active class discriminator")
        }
    }

    @Test
    fun testNonOptionalBucketNoExtras() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"a":1}"""
        val result = json.decodeFromString<NonOptionalBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(emptyMap(), result.extras)
    }

    @Test
    fun testNamingStrategyEncodeCollision() = parametrizedTest { mode ->
        val json = Json(default) { namingStrategy = JsonNamingStrategy.SnakeCase; encodeDefaults = true }
        val data = WithNaming(someValue = 1, extras = mapOf("some_value" to JsonPrimitive(2)))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testSiblingSameTypeBuckets() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"first":{"a":1,"x":10},"second":{"a":2,"y":20}}"""
        val result = json.decodeFromString<TwoSiblings>(input, mode)
        assertEquals(1, result.first.a)
        assertEquals(JsonPrimitive(10), result.first.extras["x"])
        assertEquals(2, result.second.a)
        assertEquals(JsonPrimitive(20), result.second.extras["y"])
    }

    @Test
    fun testNestedSameDescriptorDecode() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val input = """{"value":1,"child":{"value":2,"inner":"in"},"outer":"out"}"""
        val result = json.decodeFromString<RecursiveNode>(input, mode)
        assertEquals(1, result.value)
        assertEquals(JsonPrimitive("in"), result.child?.extras?.get("inner"))
        assertEquals(JsonPrimitive("out"), result.extras["outer"])
    }

    @Test
    fun testEncodeCollisionAliasName() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val data = WithAlias(a = 1, b = "value", extras = mapOf("b_alias" to JsonPrimitive("conflict")))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testOuterBucketAfterNestedPolymorphic() = parametrizedTest { mode ->
        val json = Json(default) { encodeDefaults = true }
        val data = OuterWithNestedPoly(base = Derived(a = 1), extras = mapOf("type" to JsonPrimitive("foo")))
        // "type" is the discriminator of Derived, but OuterWithNestedPoly is not polymorphic,
        // so its extras may legally contain "type".
        val serialized = json.encodeToString(data, mode)
        val deserialized = json.decodeFromString<OuterWithNestedPoly>(serialized, mode)
        assertEquals(JsonPrimitive("foo"), deserialized.extras["type"])
    }

    // ---- bucket value types other than JsonElement ----

    @Serializable
    data class IntBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, Int> = emptyMap()
    )

    @Serializable
    data class Inner(val name: String, val count: Int)

    @Serializable
    data class ClassBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, Inner> = emptyMap()
    )

    @Serializable
    sealed class Shape

    @Serializable
    @SerialName("circle")
    data class Circle(val radius: Double) : Shape()

    @Serializable
    @SerialName("square")
    data class Square(val side: Double) : Shape()

    @Serializable
    data class ShapeBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, Shape> = emptyMap()
    )

    // Polymorphic outer with a bucket whose value type's property name collides
    // with the outer's class discriminator ("type"). Inner's "type" field must
    // be written inside the inner object — not be flagged as a bucket-key
    // collision against the outer discriminator.
    @Serializable
    sealed class PolyOuter

    @Serializable
    data class ValueWithTypeProperty(val type: String, val payload: String)

    @Serializable
    @SerialName("polyOuter")
    data class PolyOuterImpl(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, ValueWithTypeProperty> = emptyMap()
    ) : PolyOuter()

    // Contextual value type — resolution at encode/decode time via SerializersModule.
    data class ExternalValue(val text: String)

    private object ExternalValueSerializer : KSerializer<ExternalValue> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ExternalValue", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: ExternalValue) =
            encoder.encodeString(value.text)
        override fun deserialize(decoder: Decoder): ExternalValue =
            ExternalValue(decoder.decodeString())
    }

    @Serializable
    data class ContextualBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, @Contextual ExternalValue> = emptyMap()
    )

    // Inline String key — must be rejected by the strict identity check
    // because the encode path casts each entry key to String.
    @JvmInline
    @Serializable
    value class WrappedKey(val raw: String)

    @Serializable
    data class InlineKeyBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<WrappedKey, Int> = emptyMap()
    )

    @Test
    fun testRoundTripMapStringInt() = parametrizedTest { mode ->
        val input = """{"a":1,"x":10,"y":20}"""
        val result = default.decodeFromString<IntBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(10, result.extras["x"])
        assertEquals(20, result.extras["y"])
        val roundTrip = default.encodeToString(result, mode)
        assertEquals(result, default.decodeFromString<IntBucket>(roundTrip, mode))
    }

    @Test
    fun testRoundTripMapStringDataClass() = parametrizedTest { mode ->
        val input = """{"a":1,"foo":{"name":"hello","count":3},"bar":{"name":"world","count":7}}"""
        val result = default.decodeFromString<ClassBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(Inner("hello", 3), result.extras["foo"])
        assertEquals(Inner("world", 7), result.extras["bar"])
        val roundTrip = default.encodeToString(result, mode)
        assertEquals(result, default.decodeFromString<ClassBucket>(roundTrip, mode))
    }

    @Test
    fun testRoundTripEmptyIntBucket() = parametrizedTest { mode ->
        val input = """{"a":1}"""
        val result = default.decodeFromString<IntBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(emptyMap(), result.extras)
        // Empty bucket: encode should not emit anything for `extras`.
        val roundTrip = default.encodeToString(result, mode)
        assertEquals(result, default.decodeFromString<IntBucket>(roundTrip, mode))
    }

    @Test
    fun testTypeMismatchInBucketValueReportsError() = parametrizedTest { mode ->
        val input = """{"a":1,"x":"not-an-int"}"""
        assertFailsWith<SerializationException> {
            default.decodeFromString<IntBucket>(input, mode)
        }
    }

    @Test
    fun testRoundTripMapStringPolymorphic() = parametrizedTest { mode ->
        val data = ShapeBucket(
            a = 1,
            extras = mapOf(
                "shape1" to Circle(radius = 2.5),
                "shape2" to Square(side = 4.0)
            )
        )
        val serialized = default.encodeToString(data, mode)
        val restored = default.decodeFromString<ShapeBucket>(serialized, mode)
        assertEquals(data, restored)
    }

    @Test
    fun testInnerValuePropertyNamesDoNotClashWithOuterDiscriminator() = parametrizedTest { mode ->
        // PolyOuterImpl's discriminator at outer level is "type". Inner value
        // ValueWithTypeProperty also has a property named "type" — that lives
        // inside the inner object's braces and must NOT be misinterpreted as
        // a bucket-key collision.
        val data: PolyOuter = PolyOuterImpl(
            a = 1,
            extras = mapOf("foo" to ValueWithTypeProperty(type = "X", payload = "Y"))
        )
        val serialized = default.encodeToString(PolyOuter.serializer(), data, mode)
        val restored = default.decodeFromString(PolyOuter.serializer(), serialized, mode)
        assertEquals(data, restored)
    }

    @Test
    fun testRoundTripContextualValue() = parametrizedTest { mode ->
        val json = Json(default) {
            serializersModule = SerializersModule {
                contextual(ExternalValue::class, ExternalValueSerializer)
            }
        }
        val data = ContextualBucket(a = 1, extras = mapOf("foo" to ExternalValue("hello")))
        val serialized = json.encodeToString(data, mode)
        val restored = json.decodeFromString<ContextualBucket>(serialized, mode)
        assertEquals(data, restored)
    }

    @Test
    fun testCaptureWinsOverIgnoreUnknownKeysForIntBucket() = parametrizedTest { mode ->
        // Capture wins over ignoreUnknownKeys = true for typed buckets,
        // parity with the JsonElement case.
        val json = Json(default) { ignoreUnknownKeys = true }
        val input = """{"a":1,"x":42}"""
        val result = json.decodeFromString<IntBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(42, result.extras["x"])
    }

    @Test
    fun testRejectInlineStringKey() {
        // Inline value class wrapping String has STRING kind but a different
        // runtime type — the strict identity check rejects it to avoid
        // runtime cast failures in the encode wrapper.
        assertFailsWith<SerializationException> {
            default.decodeFromString<InlineKeyBucket>("""{"a":1}""")
        }
    }
}
