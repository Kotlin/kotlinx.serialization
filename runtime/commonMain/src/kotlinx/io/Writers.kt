/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io


const val message = "These classes accidentally slipped to the public API surface. " +
        "We neither had intent to provide a production-quality implementation nor have an intent to support them." +
        "They are removed and to migrate, you can either use a corresponding java.io type or just a copy-paste implementation from the GitHub." +
        "If you have a use-case for multiplatform IO, please report it to the https://github.com/Kotlin/kotlinx-io/issues"

@Deprecated(message = message, level = DeprecationLevel.ERROR)
class Writer

@Deprecated(message = message, level = DeprecationLevel.ERROR)
class PrintWriter

@Deprecated(message = message, level = DeprecationLevel.ERROR)
class StringWriter

@Deprecated(message = message, level = DeprecationLevel.ERROR)
class Reader

@Deprecated(message = message, level = DeprecationLevel.ERROR)
class StringReader
