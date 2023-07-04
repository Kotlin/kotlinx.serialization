/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

/**
 * Specifies protobuf field number (a unique number for a field in the protobuf message)
 * assigned to a Kotlin property.
 *
 * See [Assigning field numbers](https://protobuf.dev/programming-guides/proto2/#assigning) for details.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoNumber(public val number: Int)

/**
 * Represents a number format in protobuf encoding set by [ProtoType] annotation.
 *
 * [DEFAULT] is default varint encoding (intXX),
 * [SIGNED] is signed ZigZag representation (sintXX), and
 * [FIXED] is fixedXX type.
 * uintXX and sfixedXX are not supported yet.
 *
 * See [Scalar value types](https://protobuf.dev/programming-guides/proto2/#scalar) for details.
 */
@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
@ExperimentalSerializationApi
public enum class ProtoIntegerType(internal val signature: Long) {
    DEFAULT(0L shl 33),
    SIGNED(1L shl 33),
    FIXED(2L shl 33);
}

/**
 * Instructs to use a particular [ProtoIntegerType] for a property of integer number type.
 * Affect [Byte], [Short], [Int], [Long] and [Char] properties and does not affect others.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoType(public val type: ProtoIntegerType)


/**
 * Instructs that a particular collection should be written as a [packed array](https://protobuf.dev/programming-guides/encoding/#packed).
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoPacked
