/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
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
 * Any serializable class can be serialized to or from [JsonElement] with [Json.fromJson] and [Json.toJson] respectively or
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
 * val stringOutput: String = json.stringify(instance)
 *
 * // JsonElement serialization specific for Json only
 * val jsonTree: JsonElement = json.toJson(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = json.parse<DataHolder>(stringOutput)
 *
 * // Deserialize from json tree, Json-specific
 * val deserializedFromTree: DataHolder = json.fromJson<DataHolder>(jsonTree)
 *
 *  // Deserialize from string to json tree, Json-specific
 *  val deserializedToTree: JsonElement = json.fromJson<JsonElement>(stringOutput)
 * ```
 *
 * Note that `@ImplicitReflectionSerializer` are used in order to omit `DataHolder.serializer`, but this is a temporary limitation.
 */
public class Json

/**
 * Default Json constructor not marked as unstable API.
 * To configure Json format behavior while still using only stable API it is possible to use `JsonConfiguration.copy` factory:
 * ```
 * val json = Json(configuration: = JsonConfiguration.Stable.copy(prettyPrint = true))
 * ```
 */
public constructor(
    @JvmField internal val configuration: JsonConfiguration = JsonConfiguration.Stable,
    context: SerialModule = EmptyModule
) : StringFormat {
    override val context: SerialModule = context + defaultJsonModule

    /**
     * DSL-like constructor for Json.
     * This constructor is marked with unstable default: its default parameters values and behaviour may change in the next releases.
     */
    @UnstableDefault
    public constructor(block: JsonBuilder.() -> Unit) : this(JsonBuilder().apply { block() })

    @UseExperimental(UnstableDefault::class)
    @Deprecated(
        message = "Default constructor is deprecated, please specify the desired configuration explicitly or use Json(JsonConfiguration.Default)",
        replaceWith = ReplaceWith("Json(JsonConfiguration.Default)"),
        level = DeprecationLevel.ERROR
    )
    public constructor() : this(JsonConfiguration(useArrayPolymorphism = true))

    @UseExperimental(UnstableDefault::class)
    private constructor(builder: JsonBuilder) : this(builder.buildConfiguration(), builder.buildModule())

    init {
        validateConfiguration()
    }

    /**
     * Serializes [value] into an equivalent JSON using provided [serializer].
     * @throws [JsonException] if given value can not be encoded
     * @throws [SerializationException] if given value can not be serialized
     */
    public override fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String {
        val result = StringBuilder()
        val encoder = StreamingJsonOutput(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        encoder.encode(serializer, value)
        return result.toString()
    }

    /**
     * Serializes [value] into an equivalent [JsonElement] using provided [serializer].
     * @throws [JsonException] if given value can not be encoded
     * @throws [SerializationException] if given value can not be serialized
     */
    public fun <T> toJson(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(value, serializer)
    }

    /**
     * Serializes [value] into an equivalent [JsonElement] using serializer registered in the module.
     * @throws [JsonException] if given value can not be encoded
     * @throws [SerializationException] if given value can not be serialized
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> toJson(value: T): JsonElement {
        return toJson(context.getContextualOrDefault(T::class), value)
    }

    /**
     * Deserializes given json [string] into a corresponding object of type [T] using provided [deserializer].
     * @throws [JsonException] in case of malformed json
     * @throws [SerializationException] if given input can not be deserialized
     */
    public override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T {
        val reader = JsonReader(string)
        val input = StreamingJsonInput(this, WriteMode.OBJ, reader)
        val result = input.decode(deserializer)
        if (!reader.isDone) { error("Reader has not consumed the whole input: $reader") }
        return result
    }

    /**
     * Deserializes given json [string] into a corresponding [JsonElement] representation.
     * @throws [JsonException] in case of malformed json
     * @throws [SerializationException] if given input can not be deserialized
     */
    public fun parseJson(string: String): JsonElement {
        return parse(JsonElementSerializer, string)
    }

    /**
     * Deserializes [json] element into a corresponding object of type [T] using provided [deserializer].
     * @throws [JsonException] in case of malformed json
     * @throws [SerializationException] if given input can not be deserialized
     */
    public fun <T> fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T {
        return readJson(json, deserializer)
    }

    /**
     * Deserializes [json] element into a corresponding object of type [T] using serializer registered in the module.
     * @throws [JsonException] in case of malformed json
     * @throws [SerializationException] if given input can not be deserialized
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> fromJson(tree: JsonElement): T = fromJson(context.getContextualOrDefault(T::class), tree)

    /**
     * The default instance of [Json] in the form of companion object.
     */
    @UnstableDefault
    public companion object Default : StringFormat {
        private const val message =
            "Top-level JSON instances are deprecated for removal in the favour of user-configured one. " +
                    "You can either use a Json top-level object, configure your own instance  via 'Json {}' builder-like constructor, " +
                    "'Json(JsonConfiguration)' constructor or by tweaking stable configuration 'Json(JsonConfiguration.Stable.copy(prettyPrint = true))'"

        @UnstableDefault
        @Deprecated(message = message, level = DeprecationLevel.WARNING)
        public val plain = Json(JsonConfiguration(useArrayPolymorphism = true))
        @UnstableDefault
        @Deprecated(message = message, level = DeprecationLevel.WARNING)
        public val unquoted = Json(
            JsonConfiguration(
                isLenient = true,
                ignoreUnknownKeys = true,
                serializeSpecialFloatingPointValues = true,
                unquotedPrint = true,
                useArrayPolymorphism = true
            )
        )

        @UnstableDefault
        @Deprecated(message = message, level = DeprecationLevel.WARNING)
        public val indented = Json(JsonConfiguration(prettyPrint = true, useArrayPolymorphism = true))

        @UnstableDefault
        @Deprecated(message = message, level = DeprecationLevel.WARNING)
        public val nonstrict = Json(
            JsonConfiguration(
                isLenient = true,
                ignoreUnknownKeys = true,
                serializeSpecialFloatingPointValues = true,
                useArrayPolymorphism = true
            )
        )

        private val jsonInstance = Json(JsonConfiguration.Default)

        override val context: SerialModule
            get() = jsonInstance.context

        override fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String =
            jsonInstance.stringify(serializer, value)
        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
            jsonInstance.parse(deserializer, string)

        /**
         * @see Json.toJson
         */
        public fun <T> toJson(serializer: SerializationStrategy<T>, value: T): JsonElement {
            return jsonInstance.writeJson(value, serializer)
        }

        /**
         * @see Json.toJson
         */
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> toJson(value: T): JsonElement {
            return toJson(context.getContextualOrDefault(T::class), value)
        }

        /**
         * @see Json.parseJson
         */
        public fun parseJson(string: String): JsonElement {
            return parse(JsonElementSerializer, string)
        }

        /**
         * @see Json.fromJson
         */
        public fun <T> fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T {
            return jsonInstance.readJson(json, deserializer)
        }

        /**
         * @see Json.fromJson
         */
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> fromJson(tree: JsonElement): T =
            fromJson(context.getContextualOrDefault(T::class), tree)
    }

    private fun validateConfiguration() {
        if (configuration.useArrayPolymorphism) return
        val collector = ContextValidator(configuration.classDiscriminator)
        context.dumpTo(collector)
    }
}

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
        JsonLiteral::class to JsonLiteralSerializer,
        JsonNull::class to JsonNullSerializer,
        JsonObject::class to JsonObjectSerializer,
        JsonArray::class to JsonArraySerializer
    )
)

internal const val lenientHint = "Use 'JsonConfiguration.isLenient = true' to accept non-compliant JSON"
