/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * The main entry point to work with JSON serialization.
 * Is is typically used by constructing an application-specific instance, registering
 * custom serializers via [Json.install] and then using it either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [JsonElement] back and forth.
 *
 * This is the only serial format which has first-class [JsonElement] support.
 * Any serializable class can be serialized to or from [JsonElement] with [Json.fromJson] and [Json.toJson] respectively or
 * serialize properties of [JsonElement] type.
 *
 * Json-specific behaviour can be configured with [JsonConfiguration].
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json()
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
 * val json = Json(configuration: = JsonConfiguration.Stable.copy(strictMode = false))
 * ```
 */
public constructor(
    @JvmField internal val configuration: JsonConfiguration = JsonConfiguration.Stable,
    context: SerialModule = EmptyModule
): AbstractSerialFormat(context + defaultJsonModule), StringFormat {

    /**
     * DSL-like constructor for Json.
     * This constructor is marked with unstable default: its default parameters values and behaviour may change in the next releases.
     */
    @UnstableDefault
    public constructor(block: JsonBuilder.() -> Unit) : this(JsonBuilder().apply { block() })

    @Deprecated(
        message = "Default constructor is deprecated, please specify desired configuration explicitly or use Json(JsonConfiguration.Default)",
        replaceWith = ReplaceWith("Json(JsonConfiguration.Default)"),
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION_ERROR")
    public constructor() : this(unquoted = false)

    @UseExperimental(UnstableDefault::class)
    private constructor(builder: JsonBuilder) : this(builder.buildConfiguration(), builder.buildModule())

    @UseExperimental(UnstableDefault::class)
    @Deprecated(
        message = "Use constructor with JsonConfiguration instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("Json(JsonConfiguration(encodeDefaults, strictMode, unquoted, indented, indent, useArrayPolymorphism), context)")
    )
    public constructor(
        unquoted: Boolean = false,
        indented: Boolean = false,
        indent: String = "    ",
        strictMode: Boolean = true,
        updateMode: UpdateMode = UpdateMode.OVERWRITE,
        encodeDefaults: Boolean = true,
        context: SerialModule = EmptyModule
    ) : this(
        JsonConfiguration(
            encodeDefaults,
            strictMode,
            unquoted,
            indented,
            indent,
            useArrayPolymorphism = true,
            updateMode = updateMode
        ), context
    )


    /**
     * Serializes [obj] into an equivalent JSON using provided [serializer].
     * @throws [JsonException] subclass in case of serialization error.
     */
    public override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String {
        val result = StringBuilder()
        val encoder = StreamingJsonOutput(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        encoder.encode(serializer, obj)
        return result.toString()
    }

    /**
     * Serializes [value] into an equivalent [JsonElement] using provided [serializer].
     * @throws [JsonException] subclass in case of serialization error.
     */
    public fun <T> toJson(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(value, serializer)
    }

    /**
     * Serializes [value] into an equivalent [JsonElement] using serializer registered in the module.
     * @throws [JsonException] subclass in case of serialization error.
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> toJson(value: T): JsonElement {
        return toJson(context.getContextualOrDefault(T::class), value)
    }

    /**
     * Deserializes given json [string] into a corresponding object of type [T] using provided [deserializer].
     * @throws [JsonException] subclass in case of serialization error.
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
     * @throws [JsonException] subclass in case of serialization error.
     */
    public fun parseJson(string: String): JsonElement {
        return parse(JsonElementSerializer, string)
    }

    /**
     * Deserializes [json] element into a corresponding object of type [T] using provided [deserializer].
     * @throws [JsonException] subclass in case of serialization error.
     */
    public fun <T> fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T {
        return readJson(json, deserializer)
    }

    /**
     * Deserializes [json] element into a corresponding object of type [T] using serializer registered in the module.
     * @throws [JsonException] subclass in case of serialization error.
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> fromJson(tree: JsonElement): T = fromJson(context.getContextualOrDefault(T::class), tree)

    companion object : StringFormat {
        @Suppress("DEPRECATION")
        @UnstableDefault
        public val plain = Json()
        @UnstableDefault
        public val unquoted = Json(JsonConfiguration(unquoted = true, useArrayPolymorphism = true))
        @UnstableDefault
        public val indented = Json(JsonConfiguration(prettyPrint = true, useArrayPolymorphism = true))
        @UnstableDefault
        public val nonstrict = Json(JsonConfiguration(strictMode = false, useArrayPolymorphism = true))

        override fun install(module: SerialModule) = throw IllegalStateException("You should not install anything to global instance")
        @UseExperimental(UnstableDefault::class)
        override val context: SerialModule get() = plain.context

        @UnstableDefault
        override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String =
            plain.stringify(serializer, obj)

        @UnstableDefault
        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
            plain.parse(deserializer, string)
    }
}

/**
 * Builder to conveniently build Json instances.
 * Properties of this builder are directly matched with properties of [JsonConfiguration].
 */
@UnstableDefault
public class JsonBuilder {
    public var encodeDefaults: Boolean = true
    public var strictMode: Boolean = true
    public var unquoted: Boolean = false
    public var prettyPrint: Boolean = false
    public var indent: String = "    "
    public var useArrayPolymorphism: Boolean = false
    public var classDiscriminator: String = "type"
    public var serialModule: SerialModule = EmptyModule

    public fun buildConfiguration(): JsonConfiguration =
        JsonConfiguration(encodeDefaults, strictMode, unquoted, prettyPrint, indent, useArrayPolymorphism, classDiscriminator)

    public fun buildModule(): SerialModule = serialModule
}

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
