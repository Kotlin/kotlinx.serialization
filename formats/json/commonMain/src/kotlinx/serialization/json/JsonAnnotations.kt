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
 * println(project)
 * val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
 * println(oldProject)
 * ```
 *
 * This annotation has lesser priority than [SerialName].
 *
 * @see JsonBuilder.useAlternativeNames
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class JsonNames(vararg val names: String)

/**
 * Specifies key for class discriminator value used during polymorphic serialization in [Json].
 * Provided key is used only for an annotated class, to configure global class discriminator, use [JsonBuilder.classDiscriminator]
 * property.
 *
 * It is possible to define different class discriminators for different parts of class hierarchy.
 * Pay attention to the fact that class discriminator, same as polymorphic serializer's base class, is
 * determined statically.
 *
 * Example:
 * ```
 * @Serializable
 * @JsonClassDiscriminator("class")
 * abstract class Base
 *
 * @Serializable
 * @JsonClassDiscriminator("error_class")
 * abstract class ErrorClass: Base()
 *
 * @Serializable
 * class Message(val message: Base, val error: ErrorClass?)
 *
 * val message = Json.decodeFromString<Message>("""{"message": {"class":"my.app.BaseMessage", "message": "not found"}, "error": {"error_class":"my.app.GenericError", "error_code": 404}}""")
 * ```
 *
 * @see JsonBuilder.classDiscriminator
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi
public annotation class JsonClassDiscriminator(val discriminator: String)
