/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.descriptors.*
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
 * data class Data(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json { ignoreUnknownKeys = true }
 * val instance = Data(42, "some data", buildJsonObject { put("key", "value") })
 *
 * // Plain Json usage: returns '{"id": 42, "some data", "extensions": {"key": "value" } }'
 * val jsonString: String = json.encodeToString(instance)
 *
 * // JsonElement serialization, specific for JSON format
 * val jsonElement: JsonElement = json.encodeToJsonElement(instance)
 *
 * // Deserialize from string
 * val deserialized: Data = json.decodeFromString<Data>(jsonString)
 *
 * // Deserialize from json element, JSON-specific
 * val deserializedFromElement: Data = json.decodeFromJsonElement<Data>(jsonElement)
 *
 *  // Deserialize from string to JSON tree, JSON-specific
 * val deserializedElement: JsonElement = json.parseToJsonElement(jsonString)
 *
 * // Deserialize a stream of a single item from an input stream
 * val sequence = Json.decodeToSequence<Data>(ByteArrayInputStream(jsonString.encodeToByteArray()))
 * for (item in sequence) {
 *     println(item) // Prints deserialized Data value
 * }
 * ```
 *
 * Json instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [JsonDecoder] and [JsonEncoder] for customizable behaviour.
 *
 * Json format configuration can be refined using the corresponding constructor:
 * ```
 * val defaultJson = Json {
 *     encodeDefaults = true
 *     ignoreUnknownKeys = true
 * }
 * // Will inherit the properties of defaultJson
 * val debugEndpointJson = Json(defaultJson) {
 *     // ignoreUnknownKeys and encodeDefaults are set to true
 *     prettyPrint = true
 * }
 * ```
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
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(Json.encodeToString(data))
     * ```
     */
    @ThreadLocal // to support caching on K/N
    @OptIn(ExperimentalSerializationApi::class)
    public companion object Default : Json(JsonConfiguration(), EmptySerializersModule())

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     * This method is recommended to be used with an explicit serializer (e.g. the custom or third-party one),
     * otherwise the `encodeToString(value: T)` version might be preferred as the most concise one.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     *
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(Json.encodeToString(Project.serializer(), data))
     * // The same as Json.encodeToString<T>(value: T) overload
     * println(Json.encodeToString(data))
     * ```
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
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(Json.decodeFromString(Project.serializer(), """{"name":"kotlinx.serialization","language":"Kotlin"}"""))
     * ```
     *
     * @throws [SerializationException] if the given JSON string is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    public final override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, @FormatLanguage("json", "", "") string: String): T {
        val lexer = StringJsonLexer(this, string)
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    }

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

    /**
     * Following functions are copied from extensions on StringFormat
     * to streamline experience for newcomers, since IDE does not star-import kotlinx.serialization.* automatically
     */

    /**
     * Serializes the [value] of type [T] into an equivalent JSON using serializer
     * retrieved from the reified type parameter.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     *
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(Json.encodeToString(data))
     * ```
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    public inline fun <reified T> encodeToString(value: T): String =
        encodeToString(serializersModule.serializer(), value)

    /**
     * Decodes and deserializes the given JSON [string] to the value of type [T] using deserializer
     * retrieved from the reified type parameter.
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(Json.decodeFromString<Project>("""{"name":"kotlinx.serialization","language":"Kotlin"}"""))
     * ```
     *
     * @throws SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [T]
     */
    public inline fun <reified T> decodeFromString(@FormatLanguage("json", "", "") string: String): T =
        decodeFromString(serializersModule.serializer(), string)
}

/**
 * Description of JSON input shape used for decoding to sequence.
 *
 * The sequence represents a stream of objects parsed one by one;
 * [DecodeSequenceMode] defines a separator between these objects.
 * Typically, these objects are not separated by meaningful characters ([WHITESPACE_SEPARATED]),
 * or the whole stream is a large array of objects separated with commas ([ARRAY_WRAPPED]).
 *
 * It is used in `Json.decodeToSequence` family of functions:
 * ```
 * @Serializable
 * data class Game(val name: String)
 * val input = """{"name": "Gothic"} {"name": "Planescape"} {"name": "Fallout"}"""
 * // On multiplatform, Okio's Source can be used
 * val inputStream = ByteArrayInputStream(input.encodeToByteArray())
 *
 * val sequence = Json.decodeToSequence<Game>(inputStream, DecodeSequenceMode.WHITESPACE_SEPARATED)
 * // Prints Game(name=Gothic), Game(name=Planescape) and Game(name=Fallout)
 * for (game in sequence) {
 *     println(game)
 * }
 * ```
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
     * """[{"key": "value"},   {"key": "value2"},{"key2": "value2"}]"""
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
 *
 * Example of usage:
 * ```
 * val defaultJson = Json {
 *     encodeDefaults = true
 *     ignoreUnknownKeys = true
 * }
 * // Will inherit the properties of defaultJson
 * val debugEndpointJson = Json(defaultJson) {
 *     // ignoreUnknownKeys and encodeDefaults are set to true
 *     prettyPrint = true
 * }
 * ```
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
 * Builder of the [Json] instance provided by `Json { ... }` factory function:
 *
 * ```
 * val json = Json { // this: JsonBuilder
 *     encodeDefaults = true
 *     ignoreUnknownKeys = true
 * }
 * ```
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith")
@OptIn(ExperimentalSerializationApi::class)
public class JsonBuilder internal constructor(json: Json) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     *
     * Example:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String = "kotlin")
     *
     * // Prints {"name":"test-project"}
     * println(Json.encodeToString(Project("test-project")))
     *
     * // Prints {"name":"test-project","language":"kotlin"}
     * val withDefaults = Json { encodeDefaults = true }
     * println(withDefaults.encodeToString(Project("test-project")))
     * ```
     *
     * This option does not affect decoding.
     */
    public var encodeDefaults: Boolean = json.configuration.encodeDefaults

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled properties with `null` values are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     *
     * It is possible to make decoder treat some invalid input data as the missing field to enhance the functionality of this flag.
     * See [coerceInputValues] documentation for details.
     *
     * Example of usage:
     * ```
     * @Serializable
     * data class Project(val name: String, val description: String?)
     * val implicitNulls = Json { explicitNulls = false }
     *
     * // Encoding
     * // Prints '{"name":"unknown","description":null}'. null is explicit
     * println(Json.encodeToString(Project("unknown", null)))
     * // Prints '{"name":"unknown"}', null is omitted
     * println(implicitNulls.encodeToString(Project("unknown", null)))
     *
     * // Decoding
     * // Prints Project(name=unknown, description=null)
     * println(implicitNulls.decodeFromString<Project>("""{"name":"unknown"}"""))
     * // Fails with "MissingFieldException: Field 'description' is required"
     * Json.decodeFromString<Project>("""{"name":"unknown"}""")
     * ```
     *
     * Exercise extra caution if you want to use this flag and have non-typical classes with properties
     * that are nullable, but have default value that is not `null`. In that case, encoding and decoding will not
     * be symmetrical if `null` is omitted from the output.
     * Example of such a pitfall:
     *
     * ```
     * @Serializable
     * data class Example(val nullable: String? = "non-null default")
     *
     * val json = Json { explicitNulls = false }
     *
     * val original = Example(null)
     * val s = json.encodeToString(original)
     * // prints "{}" because of explicitNulls flag
     * println(s)
     * val decoded = json.decodeFromString<Example>(s)
     * // Prints "non-null default" because default value is inserted since `nullable` field is missing in the input
     * println(decoded.nullable)
     * println(decoded != original) // true
     * ```
     */
    public var explicitNulls: Boolean = json.configuration.explicitNulls

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     *
     * Example of usage:
     * ```
     * @Serializable
     * data class Project(val name: String)
     * val withUnknownKeys = Json { ignoreUnknownKeys = true }
     * // Project(name=unknown), "version" is ignored completely
     * println(withUnknownKeys.decodeFromString<Project>("""{"name":"unknown", "version": 2.0}"""))
     * // Fails with "Encountered an unknown key 'version'"
     * Json.decodeFromString<Project>("""{"name":"unknown", "version": 2.0}""")
     * ```
     *
     * In case you wish to allow unknown properties only for specific class(es),
     * consider using [JsonIgnoreUnknownKeys] annotation instead of this configuration flag.
     *
     * @see JsonIgnoreUnknownKeys
     */
    public var ignoreUnknownKeys: Boolean = json.configuration.ignoreUnknownKeys

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode, unquoted JSON keys and string values are allowed.
     *
     * Example of invalid JSON that is accepted with this flag set:
     * `{key: value}` can be parsed into `@Serializable class Data(val key: String)`.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid values in the input.
     *
     * `false` by default.
     */
    public var isLenient: Boolean = json.configuration.isLenient

    /**
     * Specifies whether resulting JSON should be pretty-printed: formatted and optimized for human readability.
     * `false` by default.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Key(val type: String, val opens: String)
     * val pretty = Json { prettyPrint = true }
     * /*
     *  * Prints
     *  * {
     *  *     "type": "keycard",
     *  *     "opens": "secret door"
     *  * }
     *  */
     * println(pretty.encodeToString(Key("keycard", "secret door")))
     * ```
     */
    public var prettyPrint: Boolean = json.configuration.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode.
     * Only whitespace characters are allowed: ' ', '\n', '\r' or '\t'.
     * 4 spaces by default.
     *
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    @ExperimentalSerializationApi
    public var prettyPrintIndent: String = json.configuration.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values in the following cases:
     *
     *   1. JSON value is `null` but the property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains an unknown enum member.
     *
     * Coerced values are treated as missing; they are replaced either with a default property value if it exists, or with a `null` if [explicitNulls] flag
     * is set to `false` and a property is nullable (for enums).
     *
     * Example of usage:
     * ```
     * enum class Choice { A, B, C }
     *
     * @Serializable
     * data class Example1(val a: String = "default", b: Choice = Choice.A, c: Choice? = null)
     *
     * val coercingJson = Json { coerceInputValues = true }
     * // Decodes Example1("default", Choice.A, null) instance
     * coercingJson.decodeFromString<Example1>("""{"a": null, "b": "unknown", "c": "unknown"}""")
     *
     * @Serializable
     * data class Example2(val c: Choice?)
     *
     * val coercingImplicitJson = Json(coercingJson) { explicitNulls = false }
     * // Decodes Example2(null) instance.
     * coercingImplicitJson.decodeFromString<Example1>("""{"c": "unknown"}""")
     * ```
     *
     * `false` by default.
     */
    public var coerceInputValues: Boolean = json.configuration.coerceInputValues

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * `type` by default.
     *
     * Note that if your class has any serial names that are equal to [classDiscriminator]
     * (e.g., `@Serializable class Foo(val type: String)`), an [IllegalArgumentException] will be thrown from `Json {}` builder.
     * You can disable this check and class discriminator inclusion with [ClassDiscriminatorMode.NONE], but kotlinx.serialization will not be
     * able to deserialize such data back.
     *
     * @see classDiscriminatorMode
     */
    public var classDiscriminator: String = json.configuration.classDiscriminator

    /**
     * Defines which classes and objects should have class discriminator added to the output.
     * [ClassDiscriminatorMode.POLYMORPHIC] by default.
     *
     * Other modes are generally intended to produce JSON for consumption by third-party libraries,
     * therefore, this setting does not affect the deserialization process.
     *
     * @see classDiscriminator
     */
    @ExperimentalSerializationApi
    public var classDiscriminatorMode: ClassDiscriminatorMode = json.configuration.classDiscriminatorMode

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     * `true` by default.
     */
    public var useAlternativeNames: Boolean = json.configuration.useAlternativeNames

    /**
     * Specifies [JsonNamingStrategy] that should be used for all properties in classes for serialization and deserialization.
     *
     * `null` by default.
     *
     * This strategy is applied for all entities that have [StructureKind.CLASS].
     */
    @ExperimentalSerializationApi
    public var namingStrategy: JsonNamingStrategy? = json.configuration.namingStrategy

    /**
     * Enables decoding enum values in a case-insensitive manner.
     * Encoding is not affected by this option.
     *
     * It affects both enum serial names and alternative names (specified with the [JsonNames] annotation).
     * Example of usage:
     * ```
     * enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }
     *
     * @Serializable
     * data class Outer(val enums: List<E>)
     *
     * val json = Json { decodeEnumsCaseInsensitive = true }
     * // Prints [VALUE_A, VALUE_B]
     * println(json.decodeFromString<Outer>("""{"enums":["Value_A", "alternative"]}""").enums)
     * // Will fail with SerializationException: no such enum as 'Value_A'
     * Json.decodeFromString<Outer>("""{"enums":["Value_A", "alternative"]}""")
     * ```
     *
     * With this feature enabled, it is no longer possible to decode enum values that have the same name in a lowercase form.
     * The following code will throw a serialization exception:
     * ```
     * enum class CaseSensitiveEnum { One, ONE }
     * val json = Json { decodeEnumsCaseInsensitive = true }
     * // Fails with SerializationException: The suggested name 'one' for enum value ONE is already one of the names for enum value One
     * json.decodeFromString<CaseSensitiveEnum>("ONE")
     * ```
     */
    @ExperimentalSerializationApi
    public var decodeEnumsCaseInsensitive: Boolean = json.configuration.decodeEnumsCaseInsensitive

    /**
     * Allows parser to accept trailing (ending) commas in JSON objects and arrays,
     * making inputs like `[1, 2, 3,]` and `{"key": "value",}` valid.
     * Does not affect encoding.
     * `false` by default.
     */
    @ExperimentalSerializationApi
    public var allowTrailingComma: Boolean = json.configuration.allowTrailingComma

    /**
     * Allows parser to accept C/Java-style comments in JSON input.
     *
     * Comments are being skipped and are not stored anywhere; this setting does not affect encoding in any way.
     *
     * More specifically, a comment is a substring that is not a part of JSON key or value, conforming to one of those:
     *
     * 1. Starts with `//` characters and ends with a newline character `\n`.
     * 2. Starts with `/*` characters and ends with `*/` characters. Nesting block comments
     *  is not supported: no matter how many `/*` characters you have, first `*/` will end the comment.
     *
     *  `false` by default.
     */
    @ExperimentalSerializationApi
    public var allowComments: Boolean = json.configuration.allowComments

    /**
     * Removes JSON specification restriction on special floating-point values such as `NaN` and `Infinity`
     * and enables their serialization and deserialization as float literals without quotes.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * This option affects both encoding and decoding.
     * `false` by default.
     *
     * Example of usage:
     * ```
     * val floats = listOf(1.0, 2.0, Double.NaN, Double.NEGATIVE_INFINITY)
     * val json = Json { allowSpecialFloatingPointValues = true }
     * // Prints [1.0,2.0,NaN,-Infinity]
     * println(json.encodeToString(floats))
     * // Prints [1.0, 2.0, NaN, -Infinity]
     * println(json.decodeFromString<List<Double>>("[1.0,2.0,NaN,-Infinity]"))
     * ```
     */
    public var allowSpecialFloatingPointValues: Boolean = json.configuration.allowSpecialFloatingPointValues

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    public var allowStructuredMapKeys: Boolean = json.configuration.allowStructuredMapKeys

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     *
     * This option can only be used if [classDiscriminatorMode] in a default [ClassDiscriminatorMode.POLYMORPHIC] state.
     */
    public var useArrayPolymorphism: Boolean = json.configuration.useArrayPolymorphism

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Json] instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    public var serializersModule: SerializersModule = json.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    internal fun build(): JsonConfiguration {
        if (useArrayPolymorphism) {
            require(classDiscriminator == defaultDiscriminator) {
                "Class discriminator should not be specified when array polymorphism is specified"
            }
            require(classDiscriminatorMode == ClassDiscriminatorMode.POLYMORPHIC) {
                "useArrayPolymorphism option can only be used if classDiscriminatorMode in a default POLYMORPHIC state."
            }
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

        return JsonConfiguration(
            encodeDefaults, ignoreUnknownKeys, isLenient,
            allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, allowSpecialFloatingPointValues, useAlternativeNames,
            namingStrategy, decodeEnumsCaseInsensitive, allowTrailingComma, allowComments, classDiscriminatorMode
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
        val collector = JsonSerializersModuleValidator(configuration)
        serializersModule.dumpTo(collector)
    }
}

/**
 * This accessor should be used to workaround for freezing problems in Native, see Native source set
 */
internal expect val Json.schemaCache: DescriptorSchemaCache

private const val defaultIndent = "    "
private const val defaultDiscriminator = "type"
