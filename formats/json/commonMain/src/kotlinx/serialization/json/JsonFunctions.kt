/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.FormatLanguage

/**
 * Decodes and deserializes the given JSON [string] to the value of type [T] using deserializer
 * retrieved from the reified type parameter.
 *
 * @throws SerializationException in case of any decoding-specific error
 * @throws IllegalArgumentException if the decoded input is not a valid instance of [T]
 */
public inline fun <reified T> Json.decodeFromString(@FormatLanguage("json", "", "") string: String): T =
    decodeFromString(serializersModule.serializer(), string)
