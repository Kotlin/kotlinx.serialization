/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.time.*
import java.time.format.*

/**
 * Serializer that encodes and decodes [java.time.LocalDateTime] as its string representation.
 * Intended to be used when working with java.time.LocalDatTime.
 * Serializes and deserializes LocalDateTime into String format.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
public object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.LocalDateTimeAsStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val formatted = formatter.format(value)
        encoder.encodeString(formatted)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val formatted = decoder.decodeString()
        return LocalDateTime.parse(formatted, formatter)
    }
}