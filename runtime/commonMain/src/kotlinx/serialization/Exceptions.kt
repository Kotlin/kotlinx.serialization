/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

/**
 * A generic exception indicating the problem during serialization or deserialization process
 */
public open class SerializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// thrown from generated code
/**
 * Thrown when [KSerializer] did not receive property from [Decoder], and this property was not optional.
 */
public class MissingFieldException(fieldName: String) :
    SerializationException("Field '$fieldName' is required, but it was missing")

/**
 * Thrown when [KSerializer] received unknown property index from [CompositeDecoder.decodeElementIndex].
 *
 * Usually this exception means that data schema has changed in backwards-incompatible way.
 * Index may be negative, in that case it should be equal to [CompositeDecoder.UNKNOWN_NAME].
 */
public class UnknownFieldException(index: Int) : SerializationException("Unknown field for index $index")

// thrown from generated code, deprecated with update removal
public class UpdateNotSupportedException(className: String) :
    SerializationException("Update is not supported for $className")
