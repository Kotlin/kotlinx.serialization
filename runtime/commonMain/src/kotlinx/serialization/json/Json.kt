/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.js.*

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
 *  val deserializedToTree: JsonElement = json.parseJsonElement(stringOutput)
 * ```
 */
@OptIn(ExperimentalSerializationApi::class)
public sealed class Json(internal val configuration: JsonConf) : StringFormat {

    override val serializersModule: SerializersModule
        get() = configuration.serializersModule

    /**
     * The default instance of [Json] with default configuration.
     */
    public companion object Default : Json(JsonConf())

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
}

/**
 * Creates an instance of [Json] configured from the optionally given [Json instance][from] and adjusted with [builderAction].
 */
public fun Json(from: Json = Json.Default, builderAction: JsonBuilder.() -> Unit): Json {
    val builder = JsonBuilder(from.configuration)
    builder.builderAction()
    val conf = builder.build()
    return JsonImpl(conf)
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
 * Deserializes the given [json] element into a value of type [T] using a deserialize retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON string is malformed or cannot be deserialized to the value of type [T].
 */
public inline fun <reified T> Json.decodeFromJsonElement(json: JsonElement): T =
    decodeFromJsonElement(serializersModule.serializer(), json)

/**
 * Builder of the [Json] instance provided by `Json { ... }` factory function.
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith")
public class JsonBuilder internal constructor(conf: JsonConf) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `true` by default.
     */
    public var encodeDefaults: Boolean = conf.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = conf.ignoreUnknownKeys

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
    public var isLenient: Boolean = conf.isLenient

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    public var allowStructuredMapKeys: Boolean = conf.allowStructuredMapKeys

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *  `false` by default.
     */
    public var prettyPrint: Boolean = conf.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode
     * 4 spaces by default.
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    @ExperimentalSerializationApi
    public var prettyPrintIndent: String = conf.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     *   1. JSON value is `null` but property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     *
     * `false` by default.
     */
    public var coerceInputValues: Boolean = conf.coerceInputValues

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     */
    public var useArrayPolymorphism: Boolean = conf.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    public var classDiscriminator: String = conf.classDiscriminator

    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * `false` by default.
     */
    public var allowSpecialFloatingPointValues: Boolean = conf.allowSpecialFloatingPointValues

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Json] instance.
     */
    public var serializersModule: SerializersModule = conf.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    internal fun build(): JsonConf {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) {
            require(prettyPrintIndent == defaultIndent) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != defaultIndent) {
            // Values allowed by JSON specification as whitespaces
            val allWhitespaces = prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had $prettyPrintIndent"
            }
        }

        return JsonConf(
            encodeDefaults, ignoreUnknownKeys, isLenient,
            allowStructuredMapKeys, prettyPrint, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, allowSpecialFloatingPointValues, serializersModule
        )
    }

    // Deprecated members below

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This flag was renamed to 'allowSpecialFloatingPointValues' during serialization 1.0 API stabilization",
        replaceWith = ReplaceWith("allowSpecialFloatingPointValues")
    )
    public var serializeSpecialFloatingPointValues: Boolean = false

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This flag was renamed to 'prettyPrintIndent' during serialization 1.0 API stabilization",
        replaceWith = ReplaceWith("prettyPrintIndent")
    )
    public var indent: String = "    "

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "'strictMode = true' is replaced with 3 new configuration parameters: " +
                "'ignoreUnknownKeys = false' to fail if an unknown key is encountered, " +
                "'serializeSpecialFloatingPointValues = false' to fail on 'NaN' and 'Infinity' values, " +
                "'isLenient = false' to prohibit parsing of any non-compliant or malformed JSON"
    )
    public var strictMode: Boolean = true

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "Unquoted mode was deprecated without replacement",
    )
    public var unquoted: Boolean = false

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "Unquoted mode was deprecated without replacement"
    )
    public var unquotedPrint: Boolean = false

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "'serialModule' was renamed to 'serializersModule' during serialization 1.0 API stabilization",
        replaceWith = ReplaceWith("serializersModule")
    )
    public var serialModule: SerializersModule = EmptySerializersModule

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This method was deprecated for removal during serialization 1.0 API stabilization",
    )
    @Suppress("DEPRECATION_ERROR")
    public fun buildConfiguration(): JsonConfiguration = error("Deprecated and should not be called")

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This method was deprecated for removal during serialization 1.0 API stabilization",
    )
    public fun buildModule(): SerializersModule = error("Deprecated and should not be called")
}

private class JsonImpl(configuration: JsonConf) : Json(configuration) {

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serializersModule == EmptySerializersModule) return // Fast-path for in-place JSON allocations
        val collector = PolymorphismValidator(configuration.useArrayPolymorphism, configuration.classDiscriminator)
        serializersModule.dumpTo(collector)
    }
}

// Deprecated helpers

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of Json {} builder function during serialization 1.0 API stabilization.\n" +
            "In order to migrate, please replace it with Json { } builder function. JsonBuilder receiver has the same " +
            "properties as 'JsonConfiguration' and 'context' and should be configured there.\n" +
            "Json(JsonConfiguration.Default) can be replaced with Json companion instead."
)
@Suppress("DEPRECATION_ERROR")
public fun Json(
    configuration: JsonConfiguration, context: SerializersModule = EmptySerializersModule
): Json = error("Deprecated and should not be called")


/*
 * These two declarations are trick for better migration.
 * Most of the usages (according to GitHub search) were the following:
 * ```
 * Json(JsonConfiguration.Default)
 * Json(JsonConfiguration.Stable)
 * Json(JsonConfiguration.X, module)
 * ```
 *
 * To migrate them gracefully, these properties subtype was narrowed down to `SubtypeToDetectDefaults`
 */
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of Json.Default companion during serialization 1.0 API stabilization.\n",
    replaceWith = ReplaceWith("Json")
)
public fun Json(
    configuration: SubtypeToDetectDefault
): Json = error("Deprecated and should not be called")

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of Json {} builder function during serialization 1.0 API stabilization.\n",
    replaceWith = ReplaceWith("Json { allowStructuredMapKeys = true }") // stable configuration
)
public fun Json(
    configuration: SubtypeToDetectStable
): Json = error("Deprecated and should not be called")

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of Json {} builder function during serialization 1.0 API stabilization.\n",
    replaceWith = ReplaceWith("Json { serializersModule = context }")
)
public fun Json(
    configuration: SubtypeToDetectDefault, context: SerializersModule
): Json = error("Deprecated and should not be called")

@Deprecated(
    "Deprecated in the favour of Json {} builder function during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Json { serializersModule = context }")
)
public fun Json(context: SerializersModule): Json = error("Deprecated and should not be called")

@JsName("_Json")
@Deprecated(
    "Empty constructor was deprecated in the favour of Json.Default instance during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Json")
)
public fun Json(): Json = error("Deprecated and should not be called")

private const val defaultIndent = "    "
private const val defaultDiscriminator = "type"
