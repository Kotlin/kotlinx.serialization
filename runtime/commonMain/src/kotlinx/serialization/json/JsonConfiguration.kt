/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.UpdateMode
import kotlin.jvm.*

/**
 * The class responsible for JSON-specific customizable behaviour in [Json] format.
 *
 * Options list:
 * * [encodeDefaults] specifies whether default values are encoded.
 *
 * * [ignoreUnknownKeys] ignores encounters of unknown properties in the input JSON.
 *
 * * [isLenient] removes JSON specification restriction (RFC-4627) and makes parser
 *   more liberal to the malformed input. In lenient mode quoted boolean literals,
 *   and unquoted string literals are allowed.
 *
 * * [serializeSpecialFloatingPointValues] removes JSON specification restriction on
 *   special floating-point values such as `NaN` and `Infinity` and enables their
 *   serialization. When enabling it, please ensure that the receiving party will be
 *   able to parse these special values.
 *
 * * [unquotedPrint] specifies whether keys and values should be quoted when building the
 *   JSON string. This option is intended to be used for debugging and pretty-printing,
 *   enabling it in the production code is not recommended as strings produced with
 *   this setting are not valid JSON. String values containing whitespaces and delimiter characters
 *   will be quoted anyway.
 *
 * * [allowStructuredMapKeys] enables structured objects to be serialized as map keys by
 *   changing serialized form of the map from JSON object (kv pairs) to flat array `[k1, v1, k2, v2]`.
 *
 * * [prettyPrint] specifies whether resulting JSON should be pretty-printed.
 *
 * * [indent] specifies indent string to use with [prettyPrint] mode.
 *
 * * [useArrayPolymorphism] switches polymorphic serialization to the default array format.
 *   This is an option for legacy JSON format and should not be generally used.
 *
 * * [classDiscriminator] name of the class descriptor property in polymorphic serialization.
 *
 * This class is marked with [UnstableDefault]: its semantics may be changes in the next releases, e.g.
 * additional flag may be introduced or default parameter values may be changed.
 */
public data class JsonConfiguration @UnstableDefault constructor(
    internal val encodeDefaults: Boolean = true,
    internal val ignoreUnknownKeys: Boolean = false,
    internal val isLenient: Boolean = false,
    internal val serializeSpecialFloatingPointValues: Boolean = false,
    internal val allowStructuredMapKeys: Boolean = false,
    internal val prettyPrint: Boolean = false,
    internal val unquotedPrint: Boolean = false,
    internal val indent: String = defaultIndent,
    internal val useArrayPolymorphism: Boolean = false,
    internal val classDiscriminator: String = defaultDiscriminator,
    @Deprecated(message = "Custom update modes are not fully supported", level = DeprecationLevel.WARNING)
    internal val updateMode: UpdateMode = UpdateMode.OVERWRITE
) {

    init {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) require(indent == defaultIndent) {
            "Indent should not be specified when default printing mode is used"
        }

    }

    companion object {
        @JvmStatic
        private val defaultIndent = "    "
        @JvmStatic
        private val defaultDiscriminator = "type"

        /**
         * Default recommended configuration for [Json] format.
         *
         * This configuration is a recommended way to configure JSON by default, but due to the library evolution (until it hits 1.0.0 version)
         * we can't guarantee backwards compatibility and it is likely to change.
         */
        @JvmStatic
        @UnstableDefault
        public val Default = JsonConfiguration()

        /**
         * Stable [JsonConfiguration] that is guaranteed to preserve its semantics between releases.
         * To have a stable base in your [Json] configuration you can use `Stable.copy(param = ...)`
         */
        @OptIn(UnstableDefault::class)
        @JvmStatic
        public val Stable = JsonConfiguration(
            encodeDefaults = true,
            ignoreUnknownKeys = false,
            isLenient = false,
            serializeSpecialFloatingPointValues = false,
            allowStructuredMapKeys = true,
            prettyPrint = false,
            unquotedPrint = false,
            indent = defaultIndent,
            useArrayPolymorphism = false,
            classDiscriminator = defaultDiscriminator
        )
    }
}

@Deprecated(level = DeprecationLevel.ERROR, message = "This named parameters were deprecated." +
        "Intstead of 'unquoted' please use 'unquotedPrint'. 'strictMode' was splitted into" +
        "'ignoreUnknownKeys', 'isLenient' and 'serializeSpecialFloatingPointValues'")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
public fun JsonConfiguration(strictMode: Boolean = true, unquoted: Boolean = false) {
    error("Should not be called")
}
