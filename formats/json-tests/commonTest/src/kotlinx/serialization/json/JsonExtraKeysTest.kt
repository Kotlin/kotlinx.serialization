/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.test.checkSerializationException
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JsonExtraKeysTest : JsonTestBase() {

    // @JsonExtraKeys processing is off by default and must be enabled explicitly.
    private val extraJson = Json(default) { useExtraKeys = true }

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
    data class PlainChild(val x: Int)

    @Serializable
    data class BucketWithPlainChild(
        val a: Int,
        val child: PlainChild,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

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
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"a":1,"b":"text","c":[1,2],"d":{"x":true},"e":null}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive("text"), result.extras["b"])
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))), result.extras["c"])
        assertEquals(JsonObject(mapOf("x" to JsonPrimitive(true))), result.extras["d"])
        assertEquals(JsonNull, result.extras["e"])

        // Write-back: captured entries are emitted after regular properties,
        // reproducing the original document exactly.
        val encoded = json.encodeToString(result, mode)
        assertEquals(input, encoded)
        assertEquals(result, json.decodeFromString<Basic>(encoded, mode))
    }

    @Test
    fun testCaptureWinsOverIgnoreUnknownKeys() = parametrizedTest { mode ->
        val json = Json(extraJson) { ignoreUnknownKeys = true; encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testCaptureWinsOverAnnotation() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<IgnoresUnknown>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testJsonNamesInteraction() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"a":1,"b_alias":"value","unknown":42}"""
        val result = json.decodeFromString<WithAlias>(input, mode)
        assertEquals(1, result.a)
        assertEquals("value", result.b)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testNamingStrategy() = parametrizedTest { mode ->
        val json = Json(extraJson) { namingStrategy = JsonNamingStrategy.SnakeCase; encodeDefaults = true }
        val input = """{"some_value":1,"some_other":42}"""
        val result = json.decodeFromString<WithNaming>(input, mode)
        assertEquals(1, result.someValue)
        assertEquals(JsonPrimitive(42), result.extras["some_other"])
    }

    @Test
    fun testRejectJsonNamesOnBucket() {
        val json = Json(extraJson) { encodeDefaults = true }
        assertFailsWith<SerializationException> {
            json.decodeFromString<BadBucket>("""{"a":1}""")
        }
    }

    @Test
    fun testExplicitNullsFalse() = parametrizedTest { mode ->
        val json = Json(extraJson) { explicitNulls = false; encodeDefaults = true }
        val input = """{"a":1,"unknown":42}"""
        val result = json.decodeFromString<WithNullable>(input, mode)
        assertEquals(1, result.a)
        assertNull(result.b)
        assertEquals(JsonPrimitive(42), result.extras["unknown"])
    }

    @Test
    fun testDiscriminatorExcluded() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"base":{"type":"derived","a":1,"unknown":42}}"""
        val result = json.decodeFromString<Wrapper>(input, mode)
        val derived = result.base as Derived
        assertEquals(1, derived.a)
        assertEquals(JsonPrimitive(42), derived.extras["unknown"])
        assertNull(derived.extras["type"])
    }

    @Test
    fun testBucketOwnNameCaptured() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"a":1,"extras":42}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(42), result.extras["extras"])
        // The bucket's own property name is never written by encode, so a
        // captured key equal to it is not a collision and round-trips exactly.
        val encoded = json.encodeToString(result, mode)
        assertEquals(input, encoded)
        assertEquals(result, json.decodeFromString<Basic>(encoded, mode))
    }

    @Test
    fun testEncodeWritesExtrasLast() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val data = Basic(a = 1, extras = mapOf("z" to JsonPrimitive(2), "y" to JsonPrimitive(3)))
        assertEquals("""{"a":1,"z":2,"y":3}""", json.encodeToString(data, mode))
    }

    @Test
    fun testEncodeCollisionDeclaredName() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val data = Basic(a = 1, extras = mapOf("a" to JsonPrimitive(2)))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testEncodeCollisionDiscriminator() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val data: Base = Derived(a = 1, extras = mapOf("type" to JsonPrimitive("foo")))
        checkSerializationException({
            json.encodeToString(Base.serializer(), data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with the class discriminator")
        }
    }

    @Test
    fun testEncodeCollisionAliasName() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val data = WithAlias(a = 1, b = "value", extras = mapOf("b_alias" to JsonPrimitive("conflict")))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testNonOptionalBucketNoExtras() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"a":1}"""
        val result = json.decodeFromString<NonOptionalBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(emptyMap(), result.extras)
    }

    @Test
    fun testNamingStrategyEncodeCollision() = parametrizedTest { mode ->
        val json = Json(extraJson) { namingStrategy = JsonNamingStrategy.SnakeCase; encodeDefaults = true }
        val data = WithNaming(someValue = 1, extras = mapOf("some_value" to JsonPrimitive(2)))
        checkSerializationException({
            json.encodeToString(data, mode)
        }) { msg ->
            assertContains(msg, "conflicts with a declared property name")
        }
    }

    @Test
    fun testSiblingSameTypeBuckets() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"first":{"a":1,"x":10},"second":{"a":2,"y":20}}"""
        val result = json.decodeFromString<TwoSiblings>(input, mode)
        assertEquals(1, result.first.a)
        assertEquals(JsonPrimitive(10), result.first.extras["x"])
        assertEquals(2, result.second.a)
        assertEquals(JsonPrimitive(20), result.second.extras["y"])
    }

    @Test
    fun testCaptureSurvivesNestedPlainObject() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        // Keys captured before a nested bucket-less object must survive the
        // nested object's endStructure (decoder instance reuse).
        val input = """{"u1":1,"a":1,"child":{"x":2},"u2":2}"""
        val result = json.decodeFromString<BucketWithPlainChild>(input, mode)
        assertEquals(1, result.a)
        assertEquals(PlainChild(2), result.child)
        assertEquals(JsonPrimitive(1), result.extras["u1"])
        assertEquals(JsonPrimitive(2), result.extras["u2"])
    }

    @Test
    fun testNestedSameDescriptorDecode() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        val input = """{"value":1,"child":{"value":2,"inner":"in"},"outer":"out"}"""
        val result = json.decodeFromString<RecursiveNode>(input, mode)
        assertEquals(1, result.value)
        assertEquals(JsonPrimitive("in"), result.child?.extras?.get("inner"))
        assertEquals(JsonPrimitive("out"), result.extras["outer"])
    }

    @Test
    fun testOuterBucketAfterNestedPolymorphic() = parametrizedTest { mode ->
        val json = Json(extraJson) { encodeDefaults = true }
        // "type" is the discriminator of Derived, but OuterWithNestedPoly is not polymorphic,
        // so a top-level "type" key is a regular unknown key and lands in the outer bucket.
        // On encode, the nested Derived's discriminator must not be misattributed
        // to the outer object's bucket validation.
        val input = """{"base":{"type":"derived","a":1},"type":"foo"}"""
        val result = json.decodeFromString<OuterWithNestedPoly>(input, mode)
        assertEquals(Derived(a = 1), result.base)
        assertEquals(JsonPrimitive("foo"), result.extras["type"])
        assertEquals(input, json.encodeToString(result, mode))
    }

    // ---- JsonObject-typed bucket and rejected declarations ----

    @Serializable
    data class ObjectBucket(
        val a: Int,
        @JsonExtraKeys val extras: JsonObject = JsonObject(emptyMap())
    )

    // Typed values are not supported: the bucket must hold raw JsonElements.
    @Serializable
    data class IntBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<String, Int> = emptyMap()
    )

    // Inline String key — rejected by the strict identity check: bucket keys
    // must be plain String.
    @JvmInline
    @Serializable
    value class WrappedKey(val raw: String)

    @Serializable
    data class InlineKeyBucket(
        val a: Int,
        @JsonExtraKeys val extras: Map<WrappedKey, JsonElement> = emptyMap()
    )

    @Test
    fun testJsonObjectTypedBucket() = parametrizedTest { mode ->
        val input = """{"a":1,"x":10,"y":"s"}"""
        val result = extraJson.decodeFromString<ObjectBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(10), result.extras["x"])
        assertEquals(JsonPrimitive("s"), result.extras["y"])
        assertEquals(input, extraJson.encodeToString(result, mode))
    }

    @Test
    fun testRoundTripEmptyObjectBucket() = parametrizedTest { mode ->
        val input = """{"a":1}"""
        val result = extraJson.decodeFromString<ObjectBucket>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonObject(emptyMap()), result.extras)
        val roundTrip = extraJson.encodeToString(result, mode)
        assertEquals(result, extraJson.decodeFromString<ObjectBucket>(roundTrip, mode))
    }

    @Test
    fun testRejectTypedValues() {
        assertFailsWith<SerializationException> {
            extraJson.decodeFromString<IntBucket>("""{"a":1}""")
        }
    }

    @Test
    fun testFlagOffAnnotationIgnored() = parametrizedTest { mode ->
        val json = Json(extraJson) { useExtraKeys = false; encodeDefaults = true }
        // The bucket behaves as a regular property: read from and written
        // under its own name, no capture.
        val input = """{"a":1,"extras":{"x":10}}"""
        val result = json.decodeFromString<Basic>(input, mode)
        assertEquals(1, result.a)
        assertEquals(JsonPrimitive(10), result.extras["x"])
        assertEquals(input, json.encodeToString(result, mode))
    }

    @Test
    fun testFlagOffUnknownKeysRejected() = parametrizedTest { mode ->
        val json = Json(extraJson) { useExtraKeys = false }
        // Without capture, unknown keys fall back to default handling.
        assertFailsWith<SerializationException> {
            json.decodeFromString<Basic>("""{"a":1,"unknown":42}""", mode)
        }
    }

    @Test
    fun testFlagOffValidationSkipped() = parametrizedTest { mode ->
        // With the flag off, even invalid bucket declarations are usable as
        // plain properties — validation never runs.
        val json = Json(extraJson) { useExtraKeys = false }
        val result = json.decodeFromString<IntBucket>("""{"a":1,"extras":{"x":10}}""", mode)
        assertEquals(1, result.a)
        assertEquals(10, result.extras["x"])
    }

    @Test
    fun testRejectInlineStringKey() {
        // Inline value class wrapping String has STRING kind but a different
        // runtime type — the strict identity check rejects it.
        assertFailsWith<SerializationException> {
            extraJson.decodeFromString<InlineKeyBucket>("""{"a":1}""")
        }
    }
}
