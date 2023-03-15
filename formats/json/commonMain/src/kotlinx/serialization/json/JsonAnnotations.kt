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
@ExperimentalSerializationApi
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
@ExperimentalSerializationApi
public annotation class JsonClassDiscriminator(val discriminator: String)

/**
 * Indicates that the class has fields that should be ignored upon deserializing
 * [Json] format recognizes this annotation and is able to skip the given fields listed
 *
 * Example:
 * ```
 * @Serializable
 * @JsonIgnoreProperties("ignored_key", "other_key")
 * data class SomeClass(val key: String)
 *
 * val someClass = Json.decodeFromString<SomeClass>("""{"key":"some value"}""")
 * println(someClass) // OK
 * val otherClass = Json.decodeFromString<SomeClass>("""{"key":"some value","ignored_key":"ignored value"}""")
 * println(otherClass) // OK
 *
 * @Serializable
 * @JsonIgnoreProperties("ignored_key")
 * abstract class BaseMessage
 *
 * @Serializable
 * data class Message(val message: String) : BaseMessage
 *
 * val message = Json.decodeFromString<Message>("""{"message":"some value","ignored_key":"ignored value"}""")
 * println(message) // OK
 * ```
 */
@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi
public annotation class JsonIgnoreProperties(vararg val keys: String)
