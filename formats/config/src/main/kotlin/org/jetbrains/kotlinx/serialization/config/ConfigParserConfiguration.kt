/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlinx.serialization.config

/**
 * The class responsible for Config-specific customizable behaviour in [ConfigParser] format.
 *
 * Options list:
 * * [useConfigNamingConvention] switches naming resolution to config naming convention (hyphen separated)
 *   `someField` will be read as `some-field`
 */
public data class ConfigParserConfiguration(val useConfigNamingConvention: Boolean = false)
