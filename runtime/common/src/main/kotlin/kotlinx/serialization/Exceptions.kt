/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
package kotlinx.serialization

import kotlin.jvm.JvmOverloads

open class SerializationException @JvmOverloads constructor(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause)

class MissingFieldException(fieldName: String) : SerializationException("Field '$fieldName' is required, but it was missing")
class UnknownFieldException(index: Int): SerializationException("Unknown field for index $index")

class UpdateNotSupportedException(className: String): SerializationException("Update is not supported for $className")
