/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.internal.*
import kotlin.native.concurrent.*

/**
 * Indicates that the field can be represented in JSON
 * with multiple possible alternative names.
 * [Json] format recognizes this annotation and is able to decode
 * the data using any of the alternative names.
 *
 * Unlike [SerialName] annotation, does not affect JSON encoding in any way.
 *
 * Example of usage:
 * ```
 * @Serializable
 * data class Project(@JsonNames("title") val name: String)
 *
 * val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
 * println(project) // OK
 * val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
 * println(oldProject) // Also OK
 * ```
 *
 * This annotation has lesser priority than [SerialName].
 * In practice, this means that if property A has `@SerialName("foo")` annotation, and property B has `@JsonNames("foo")` annotation,
 * Json key `foo` will be deserialized into property A.
 *
 * Using the same alternative name for different properties across one class is prohibited and leads to a deserialization exception.
 *
 * @see JsonBuilder.useAlternativeNames
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class JsonNames(vararg val names: String)

/**
 * Specifies key for class discriminator value used during polymorphic serialization in [Json].
 * Provided key is used only for an annotated class and its subclasses;
 * to configure global class discriminator, use [JsonBuilder.classDiscriminator]
 * property.
 *
 * This annotation is [inheritable][InheritableSerialInfo], so it should be sufficient to place it on a base class of hierarchy.
 * It is not possible to define different class discriminators for different parts of class hierarchy.
 * Pay attention to the fact that class discriminator, same as polymorphic serializer's base class, is
 * determined statically.
 *
 * Example:
 * ```
 * @Serializable
 * @JsonClassDiscriminator("message_type")
 * abstract class Base
 *
 * @Serializable // Class discriminator is inherited from Base
 * abstract class ErrorClass: Base()
 *
 * @Serializable
 * class Message(val message: Base, val error: ErrorClass?)
 *
 * val message = Json.decodeFromString<Message>("""{"message": {"message_type":"my.app.BaseMessage", "message": "not found"}, "error": {"message_type":"my.app.GenericError", "error_code": 404}}""")
 * ```
 *
 * @see JsonBuilder.classDiscriminator
 */
@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi // need to take a quick look at #2142 first
public annotation class JsonClassDiscriminator(val discriminator: String)


/**
 * Specifies whether encounters of unknown properties (i.e., properties not declared in the class) in the input JSON
 * should be ignored instead of throwing [SerializationException].
 *
 * With this annotation, it is possible to allow unknown properties for annotated classes, while
 * general decoding methods (such as [Json.decodeFromString] and others) would still reject them for everything else.
 * If you want [Json.decodeFromString] allow all unknown properties for all classes and inputs, consider using
 * [JsonBuilder.ignoreUnknownKeys].
 *
 * Example:
 * ```
 * @Serializable
 * @JsonIgnoreUnknownKeys
 * class Outer(val a: Int, val inner: Inner)
 *
 * @Serializable
 * class Inner(val x: String)
 *
 * // Throws SerializationException because there is no "unknownKey" property in Inner
 * Json.decodeFromString<Outer>("""{"a":1,"inner":{"x":"value","unknownKey":"unknownValue"}}""")
 *
 * // Decodes successfully despite "unknownKey" property in Outer
 * Json.decodeFromString<Outer>("""{"a":1,"inner":{"x":"value"}, "unknownKey":42}""")
 * ```
 *
 * @see JsonBuilder.ignoreUnknownKeys
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
public annotation class JsonIgnoreUnknownKeys

/**
 * Marks a property as the bucket that captures every JSON object key that
 * does not match any declared property of the enclosing class.
 *
 * The annotated property must be of type [JsonObject] or `Map<String, JsonElement>`.
 * Captured values are stored losslessly as [JsonElement]s; apply
 * [Json.decodeFromJsonElement] to individual entries if typed access is needed.
 *
 * Unknown keys are still resolved through [JsonNames] aliases and the active
 * [JsonNamingStrategy] first; only keys that remain unmatched after that
 * resolution are placed into the bucket. The class discriminator used during
 * polymorphic decoding is also excluded from the bucket.
 *
 * On encoding, the entries of the bucket are written back as members of the
 * enclosing JSON object, after all regular properties; the bucket's own
 * property name is not written. This makes `encode(decode(input))` preserve
 * the captured keys. If the bucket was manipulated to contain a key that a
 * declared property (or the class discriminator) would be written under,
 * a [SerializationException] is thrown to prevent duplicate keys in the output.
 * In other words, the bucket may only contain keys that decoding would have
 * captured into it.
 *
 * Capturing takes precedence over both [JsonBuilder.ignoreUnknownKeys] and
 * [JsonIgnoreUnknownKeys]: when this annotation is present on a property,
 * unknown keys are always captured rather than silently dropped or rejected.
 *
 * Processing of this annotation must be enabled with the
 * [JsonBuilder.useExtraKeys] flag (`false` by default); without it, the
 * annotated property behaves as a regular property.
 *
 * Example:
 * ```
 * @Serializable
 * data class Project(
 *     val name: String,
 *     @JsonExtraKeys val extras: JsonObject = JsonObject(emptyMap())
 * )
 *
 * val json = Json { useExtraKeys = true }
 * val parsed = json.decodeFromString<Project>(
 *     """{"name":"kotlinx.serialization","stars":9000,"forks":500}"""
 * )
 * // parsed.extras == {"stars": 9000, "forks": 500}
 * // json.encodeToString(parsed) reproduces the original JSON
 * ```
 *
 * Constraints validated lazily on first use, raising [SerializationException]:
 *  - at most one property per class may be annotated with [JsonExtraKeys];
 *  - the annotated property must be of type [JsonObject] or `Map<String, JsonElement>`
 *    (with exactly the standard [String] and [JsonElement] serializers);
 *  - the property must not also carry a [JsonNames] annotation.
 *
 * It is recommended to give the annotated property a default value of
 * `JsonObject(emptyMap())` (or `emptyMap()`) so that it is optional in the input.
 *
 * @see JsonIgnoreUnknownKeys
 * @see JsonBuilder.ignoreUnknownKeys
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class JsonExtraKeys
