/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlinx.serialization.config

private const val pkg = "kotlinx.serialization.config"
private const val message = "Moved to $pkg package"

@Suppress("DEPRECATION_ERROR")
@Deprecated(message, ReplaceWith("ConfigParserConfiguration", pkg), level = DeprecationLevel.ERROR)
typealias ConfigParserConfiguration = kotlinx.serialization.config.ConfigParserConfiguration

@Deprecated(message, ReplaceWith("Hocon", pkg), level = DeprecationLevel.ERROR)
typealias ConfigParser = kotlinx.serialization.hocon.Hocon
