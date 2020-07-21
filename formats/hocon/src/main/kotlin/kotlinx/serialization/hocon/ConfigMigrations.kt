/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.config

import kotlinx.serialization.hocon.*
import kotlinx.serialization.modules.*

@Deprecated(
    "ConfigParser was renamed to Hocon during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Hocon")
)
typealias ConfigParser = Hocon

@Deprecated(
    "ConfigParserConfiguration eas deprecated during serialization 1.0 API stabilization, please use direct constructor parameters instead",
    level = DeprecationLevel.ERROR,
)
public data class ConfigParserConfiguration(val useConfigNamingConvention: Boolean = false)
