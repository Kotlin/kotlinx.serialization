/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

/**
 * A generic exception indicating the problem during serialization or deserialization process
 */
public open class SerializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// thrown from generated code
public class MissingFieldException(fieldName: String) :
    SerializationException("Field '$fieldName' is required, but it was missing")

// thrown from generated code
public class UnknownFieldException(index: Int) : SerializationException("Unknown field for index $index")

// thrown from generated code
public class UpdateNotSupportedException(className: String) :
    SerializationException("Update is not supported for $className")
