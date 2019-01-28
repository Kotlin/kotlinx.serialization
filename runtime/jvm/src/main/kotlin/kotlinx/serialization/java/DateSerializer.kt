/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import java.text.*
import java.util.*

/**
 * Serializer for [java.util.Date] which serializers date as format string in default US locale.
 */
public object DateSerializer : KSerializer<Date> {
    private val format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US)!!

    override fun serialize(encoder: Encoder, obj: Date) {
        val string = synchronized(format) { format.format(obj) }
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Date {
        val string = decoder.decodeString()
        return synchronized(format) { format.parse(string) }
    }

    override val descriptor: SerialDescriptor
        get() = StringDescriptor
}
