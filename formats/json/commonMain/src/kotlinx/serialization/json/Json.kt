/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.*

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured JSON-specific behaviour
 * and, if necessary, registered in [SerializersModule] custom serializers.
 * `Json` instance can be configured in its `Json {}` factory function using [JsonBuilder].
 * For demonstration purposes or trivial usages, Json [companion][Json.Default] can be used instead.
 *
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [JsonElement] back and forth.
 *
 * This is the only serial format which has the first-class [JsonElement] support.
 * Any serializable class can be serialized to or from [JsonElement] with [Json.decodeFromJsonElement] and [Json.encodeToJsonElement] respectively or
 * serialize properties of [JsonElement] type.
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json
 * val instance = DataHolder(42, "some data", buildJsonObject { put("additional key", "value") }
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
 *  // Deserialize from string to JSON tree, JSON-specific
 *  val deserializedToTree: JsonElement = json.parseToJsonElement(stringOutput)
 * ```
 *
 * Json instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [JsonDecoder] and [JsonEncoder] for customizable behaviour.
 */
public sealed class Json(
    public val configuration: JsonConfiguration,
    override val serializersModule: SerializersModule
) : StringFormat {

    @Deprecated(
        "Should not be accessed directly, use Json.schemaCache accessor instead",
        ReplaceWith("schemaCache"),
        DeprecationLevel.ERROR
    )
    internal val _schemaCache: DescriptorSchemaCache = DescriptorSchemaCache()

    /**
     * The default instance of [Json] with default configuration.
     */
    @ThreadLocal // to support caching
    @OptIn(ExperimentalSerializationApi::class)
    public companion object Default : Json(JsonConfiguration(), EmptySerializersModule())

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    public final override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val result = JsonToStringWriter()
        try {
            encodeByWriter(this@Json, result, serializer, value)
            return result.toString()
        } finally {
            result.release()
        }
    }

    /**
     * Decodes and deserializes the given JSON [string] to the value of type [T] using deserializer
     * retrieved from the reified type parameter.
     *
     * @throws SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [T]
     */
    public inline fun <reified T> decodeFromString(@FormatLanguage("json", "", "") string: String): T =
            decodeFromString(serializersModule.serializer(), string)

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    public final override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, @FormatLanguage("json", "", "") string: String): T {
        val lexer = createLexer(string)
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    }

    internal open fun createLexer(input: String): AbstractJsonLexer = StringJsonLexer(input)

    /**
     * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON
     */
    public fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(this@Json, value, serializer)
    }

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    public fun <T> decodeFromJsonElement(deserializer: DeserializationStrategy<T>, element: JsonElement): T {
        return readJson(this@Json, element, deserializer)
    }

    /**
     * Deserializes the given JSON [string] into a corresponding [JsonElement] representation.
     *
     * @throws [SerializationException] if the given string is not a valid JSON
     */
    public fun parseToJsonElement(@FormatLanguage("json", "", "") string: String): JsonElement {
        return decodeFromString(JsonElementSerializer, string)
    }
}

/**
 * Description of JSON input shape used for decoding to sequence.
 *
 * The sequence represents a stream of objects parsed one by one;
 * [DecodeSequenceMode] defines a separator between these objects.
 * Typically, these objects are not separated by meaningful characters ([WHITESPACE_SEPARATED]),
 * or the whole stream is a large array of objects separated with commas ([ARRAY_WRAPPED]).
 */
@ExperimentalSerializationApi
public enum class DecodeSequenceMode {
    /**
     * Declares that objects in the input stream are separated by whitespace characters.
     *
     * The stream is read as multiple JSON objects separated by any number of whitespace characters between objects. Starting and trailing whitespace characters are also permitted.
     * Each individual object is parsed lazily, when it is requested from the resulting sequence.
     *
     * Whitespace character is either ' ', '\n', '\r' or '\t'.
     *
     * Example of `WHITESPACE_SEPARATED` stream content:
     * ```
     * """{"key": "value"}{"key": "value2"}   {"key2": "value2"}"""
     * ```
     */
    WHITESPACE_SEPARATED,

    /**
     * Declares that objects in the input stream are wrapped in the JSON array.
     * Each individual object in the array is parsed lazily when it is requested from the resulting sequence.
     *
     * The stream is read as multiple JSON objects wrapped into a JSON array.
     * The stream must start with an array start character `[` and end with an array end character `]`,
     * otherwise, [JsonDecodingException] is thrown.
     *
     * Example of `ARRAY_WRAPPED` stream content:
     * ```
     * """[{"key": "value"}, {"key": "value2"},{"key2": "value2"}]"""
     * ```
     */
    ARRAY_WRAPPED,

    /**
     * Declares that parser itself should select between [WHITESPACE_SEPARATED] and [ARRAY_WRAPPED] modes.
     * The selection is performed by looking at the first meaningful character of the stream.
     *
     * In most cases, auto-detection is sufficient to correctly parse an input.
     * If the input is _whitespace-separated stream of the arrays_, parser could select an incorrect mode,
     * for that [DecodeSequenceMode] must be specified explicitly.
     *
     * Example of an exceptional case:
     * `[1, 2, 3]   [4, 5, 6]\n[7, 8, 9]`
     */
    AUTO_DETECT;
}

/**
 * Creates an instance of [Json] configured from the optionally given [Json instance][from] and adjusted with [builderAction].
 */
public fun Json(from: Json = Json.Default, builderAction: JsonBuilder.() -> Unit): Json {
    val builder = JsonBuilder(from)
    builder.builderAction()
    val conf = builder.build()
    return JsonImpl(conf, builder.serializersModule)
}

/**
 * Serializes the given [value] into an equivalent [JsonElement] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
public inline fun <reified T> Json.encodeToJsonElement(value: T): JsonElement {
    return encodeToJsonElement(serializersModule.serializer(), value)
}

/**
 * Deserializes the given [json] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 */
public inline fun <reified T> Json.decodeFromJsonElement(json: JsonElement): T =
    decodeFromJsonElement(serializersModule.serializer(), json)


/**
 * Builder of the [Json] instance provided by `Json { ... }` factory function.
 */
@Suppress("unused")
@OptIn(ExperimentalSerializationApi::class)
public class JsonBuilder internal constructor(json: Json): JsonBuilderBase(json.configuration, json.serializersModule) {
    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     *
     * `false` by default.
     */
    public var isLenient: Boolean = json.configuration.isLenient


    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * `false` by default.
     */
    public var allowSpecialFloatingPointValues: Boolean = json.configuration.allowSpecialFloatingPointValues

    /**
     * Allows parser to accept trailing (ending) commas in JSON objects and arrays,
     * making inputs like `[1, 2, 3,]` valid.
     *
     * Does not affect encoding.
     * `false` by default.
     */
    @ExperimentalSerializationApi
    public var allowTrailingComma: Boolean = json.configuration.allowTrailingComma


    @OptIn(ExperimentalSerializationApi::class)
    override fun build(): JsonConfiguration {
        validateBase()

        return JsonConfiguration(
            encodeDefaults, ignoreUnknownKeys, isLenient,
            allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, allowSpecialFloatingPointValues, useAlternativeNames,
            namingStrategy, decodeEnumsCaseInsensitive, allowTrailingComma, classDiscriminatorMode
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class JsonImpl(configuration: JsonConfiguration, module: SerializersModule) : Json(configuration, module) {

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serializersModule == EmptySerializersModule()) return // Fast-path for in-place JSON allocations
        val collector = PolymorphismValidator(configuration.useArrayPolymorphism, configuration.classDiscriminator)
        serializersModule.dumpTo(collector)
    }
}

/**
 * This accessor should be used to workaround for freezing problems in Native, see Native source set
 */
internal expect val Json.schemaCache: DescriptorSchemaCache
