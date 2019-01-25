/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.json.internal.*
import kotlin.jvm.JvmField
import kotlin.reflect.KClass

private val defaultJsonModule = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(
    JsonElement::class to JsonElementSerializer,
    JsonPrimitive::class to JsonPrimitiveSerializer,
    JsonLiteral::class to JsonLiteralSerializer,
    JsonNull::class to JsonNullSerializer,
    JsonObject::class to JsonObjectSerializer,
    JsonArray::class to JsonArraySerializer
))

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
 * Json configuration parameters:
 * [unquoted] specifies whether keys and values should be quoted, used mostly for testing.
 * [indented] specifies whether resulting JSON should be pretty-printed.
 * [indent] specifies which indent string to use with [indented] mode.
 * [strictMode] enables strict mode, which prohibits unknown keys and infinite values in floating point numbers.
 * [useArrayPolymorphism] switches polymorphic serialization to the default array format.
 * [classDiscriminator] name of the class descriptor property in polymorphic serialization.
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
public class Json(
    @JvmField internal val unquoted: Boolean = false,
    @JvmField internal val indented: Boolean = false,
    @JvmField internal val indent: String = "    ",
    @JvmField internal val strictMode: Boolean = true,
    val updateMode: UpdateMode = UpdateMode.OVERWRITE,
    val encodeDefaults: Boolean = true,
    @JvmField internal val useArrayPolymorphism: Boolean = false,
    @JvmField internal val classDiscriminator: String = "type",
    context: SerialModule = EmptyModule
): AbstractSerialFormat(context + defaultJsonModule), StringFormat {
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
        val plain = Json()
        val unquoted = Json(unquoted = true)
        val indented = Json(indented = true)
        val nonstrict = Json(strictMode = false)

        override fun install(module: SerialModule) = throw IllegalStateException("You should not install anything to global instance")
        override val context: SerialModule get() = plain.context
        override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String =
            plain.stringify(serializer, obj)

        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
            plain.parse(deserializer, string)
    }
}
