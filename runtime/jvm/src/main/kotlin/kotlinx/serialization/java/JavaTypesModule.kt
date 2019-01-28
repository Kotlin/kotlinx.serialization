/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.context.*
import java.math.*
import java.net.*
import java.util.*
import java.util.concurrent.atomic.*
import kotlin.reflect.*

/**
 * Module with external serializers for the following Java types:
 * Atomics: [AtomicInteger], [AtomicLong], [AtomicBoolean].
 * Identifiers: [URI], [URL] and [UUID].
 * Numbers: [BigInteger] and [BigDecimal].
 * Strings: [StringBuilder].
 * Dates: [java.util.Date].
 */
public object JavaTypesModule : SerialModule {
    private val javaSerializers: MutableMap<KClass<*>, KSerializer<*>> = mutableMapOf()

    init {
        javaSerializers[AtomicInteger::class] = AtomicIntegerSerializer
        javaSerializers[AtomicLong::class] = AtomicLongSerializer
        javaSerializers[AtomicBoolean::class] = AtomicBooleanSerializer
        javaSerializers[URI::class] = UriSerializer
        javaSerializers[URL::class] = UrlSerializer
        javaSerializers[UUID::class] = UuidSerializer
        javaSerializers[StringBuilder::class] = StringBuilderSerializer
        javaSerializers[BigInteger::class] = BigIntegerSerializer
        javaSerializers[BigDecimal::class] = BigDecimalSerializer
        javaSerializers[Date::class] = DateSerializer
    }

    override fun registerIn(context: MutableSerialContext) {
        javaSerializers.forEach { k, v ->
            @Suppress("UNCHECKED_CAST")
            context.registerSerializer(k as KClass<Any>, v as KSerializer<Any>)
        }
    }
}
