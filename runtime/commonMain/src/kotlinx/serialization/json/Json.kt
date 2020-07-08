/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(UnstableDefault::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured json-specific behaviour
 * ([configuration] constructor parameter) and, if necessary, registered
 * custom serializers (in [SerialModule] provided by [context] constructor parameter).
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [JsonElement] back and forth.
 *
 * This is the only serial format which has first-class [JsonElement] support.
 * Any serializable class can be serialized to or from [JsonElement] with [Json.decodeFromJsonElement] and [Json.encodeToJsonElement] respectively or
 * serialize properties of [JsonElement] type.
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json(JsonConfiguration.Default)
 * val instance = DataHolder(42, "some data", json { "additional key" to "value" })
 *
 * // Plain StringFormat usage
 * val stringOutput: String = json.encodeToString(instance)
 *
 * // JsonElement serialization specific for JSON only
 * val jsonTree: JsonElement = json.encodeToJsonElement(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = json.decodeFromString<DataHolder>(stringOutput)
 *
 * // Deserialize from json tree, JSON-specific
 * val deserializedFromTree: DataHolder = json.decodeFromJsonElement<DataHolder>(jsonTree)
 *
 *  // Deserialize from string to JSON tree, Json-specific
 *  val deserializedToTree: JsonElement = json.parseJsonElement(stringOutput)
 * ```
 */
public sealed class Json(internal val configuration: JsonConfiguration) : StringFormat {

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    public final override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val result = StringBuilder()
        val encoder = StreamingJsonEncoder(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        encoder.encodeSerializableValue(serializer, value)
        return result.toString()
    }

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string cannot be deserialized to the value of type [T].
     */
    public final override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val reader = JsonReader(string)
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, reader)
        val result = input.decodeSerializableValue(deserializer)
        if (!reader.isDone) { error("Reader has not consumed the whole input: $reader") }
        return result
    }
    /**
     * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized.
     */
    public fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(value, serializer)
    }

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string cannot be deserialized to the value of type [T].
     */
    public fun <T> decodeFromJsonElement(deserializer: DeserializationStrategy<T>, element: JsonElement): T {
        return readJson(element, deserializer)
    }

    /**
     * Deserializes the given JSON [string] into a corresponding [JsonElement] representation.
     *
     * @throws [SerializationException] if the given JSON string is malformed and cannot be deserialized
     */
    public fun parseToJsonElement(string: String): JsonElement {
        return decodeFromString(JsonElementSerializer, string)
    }

    /**
     * The default instance of [Json] with default configuration.
     */
    public companion object Default : Json(JsonConfiguration.Default) {
        override val context: SerialModule = defaultJsonModule
    }
}

/**
 * Default Json constructor .
 * To configure Json format behavior while still using only stable API it is possible to use `JsonConfiguration.copy` factory:
 * ```
 * val json = Json(configuration: = JsonConfiguration.Stable.copy(prettyPrint = true))
 * ```
 */
public fun Json(configuration: JsonConfiguration = JsonConfiguration.Stable, context: SerialModule = EmptyModule
): Json = JsonImpl(configuration, context)

/**
 * DSL-like constructor for [Json].
 */
public fun Json(block: JsonBuilder.() -> Unit): Json = JsonImpl(JsonBuilder().apply { block() })

/**
 * Serializes the given [value] into an equivalent [JsonElement] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
public inline fun <reified T : Any> Json.encodeToJsonElement(value: T): JsonElement {
    return encodeToJsonElement(context.getContextualOrDefault(), value)
}

/**
 * Deserializes the given [json] element into a value of type [T] using a deserialize retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON string is malformed or cannot be deserialized to the value of type [T].
 */
public inline fun <reified T : Any> Json.decodeFromJsonElement(tree: JsonElement): T =
    decodeFromJsonElement(context.getContextualOrDefault(), tree)

/**
 * Builder to conveniently build Json instances.
 * Properties of this builder are directly matched with properties of [JsonConfiguration].
 */
@UnstableDefault
@Suppress("unused")
public class JsonBuilder {
    public var encodeDefaults: Boolean = true
    @Deprecated(level = DeprecationLevel.ERROR,
        message = "'strictMode = true' is replaced with 3 new configuration parameters: " +
                "'ignoreUnknownKeys = false' to fail if an unknown key is encountered, " +
                "'serializeSpecialFloatingPointValues = false' to fail on 'NaN' and 'Infinity' values, " +
                "'isLenient = false' to prohibit parsing of any non-compliant or malformed JSON")
    public var strictMode: Boolean = true
    public var ignoreUnknownKeys: Boolean = false
    public var isLenient: Boolean = false
    public var serializeSpecialFloatingPointValues: Boolean = false
    @Deprecated(level = DeprecationLevel.ERROR,
        message = "'unquoted' is deprecated in the favour of 'unquotedPrint'",
        replaceWith = ReplaceWith("unquotedPrint"))
    public var unquoted: Boolean = false
    public var allowStructuredMapKeys: Boolean = false
    public var prettyPrint: Boolean = false
    public var unquotedPrint: Boolean = false
    public var indent: String = "    "
    public var coerceInputValues: Boolean = false
    public var useArrayPolymorphism: Boolean = false
    public var classDiscriminator: String = "type"
    public var serialModule: SerialModule = EmptyModule

    public fun buildConfiguration(): JsonConfiguration =
        JsonConfiguration(
            encodeDefaults,
            ignoreUnknownKeys,
            isLenient,
            serializeSpecialFloatingPointValues,
            allowStructuredMapKeys,
            prettyPrint,
            unquotedPrint,
            indent,
            coerceInputValues,
            useArrayPolymorphism,
            classDiscriminator
        )

    public fun buildModule(): SerialModule = serialModule
}

@SharedImmutable
private val defaultJsonModule = serializersModuleOf(
    mapOf<KClass<*>, KSerializer<*>>(
        JsonElement::class to JsonElementSerializer,
        JsonPrimitive::class to JsonPrimitiveSerializer,
        JsonNull::class to JsonNullSerializer,
        JsonObject::class to JsonObjectSerializer,
        JsonArray::class to JsonArraySerializer
    )
)

internal const val lenientHint = "Use 'JsonConfiguration.isLenient = true' to accept non-compliant JSON"


internal class JsonImpl(
    configuration: JsonConfiguration = JsonConfiguration.Stable,
    context: SerialModule = EmptyModule
) : Json(configuration) {
    override val context: SerialModule = context + defaultJsonModule

    constructor(builder: JsonBuilder) : this(builder.buildConfiguration(), builder.buildModule())

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (configuration.useArrayPolymorphism) return
        val collector = ContextValidator(configuration.classDiscriminator)
        context.dumpTo(collector)
    }
}