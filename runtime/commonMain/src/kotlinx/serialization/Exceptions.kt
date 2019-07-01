/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.jvm.JvmOverloads

open class SerializationException @JvmOverloads constructor(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

// these are thrown from generated code
class MissingFieldException(fieldName: String) :
    SerializationException("Field '$fieldName' is required, but it was missing")

class UnknownFieldException(index: Int) : SerializationException("Unknown field for index $index")

class UpdateNotSupportedException(className: String) : SerializationException("Update is not supported for $className")
