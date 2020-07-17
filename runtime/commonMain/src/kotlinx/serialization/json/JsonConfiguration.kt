/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.jvm.*

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This class is deprecated for removal during serialization 1.0 API stabilization.\n" +
            "For configuring Json instances, the corresponding builder function can be used instead, e.g. instead of" +
            "'Json(JsonConfiguration.Stable.copy(isLenient = true))' 'Json { isLenient = true }' should be used.\n" +
            "Instead of storing JsonConfiguration instances of the code, Json instances can be used directly:" +
            "'Json(MyJsonConfiguration.copy(prettyPrint = true))' can be replaced with 'Json(from = MyApplicationJson) { prettyPrint = true }'"
)
public open class JsonConfiguration(
    internal val encodeDefaults: Boolean = true,
    internal val ignoreUnknownKeys: Boolean = false,
    internal val isLenient: Boolean = false,
    internal val serializeSpecialFloatingPointValues: Boolean = false,
    internal val allowStructuredMapKeys: Boolean = false,
    internal val prettyPrint: Boolean = false,
    internal val unquotedPrint: Boolean = false,
    internal val indent: String = defaultIndent,
    internal val coerceInputValues: Boolean = false,
    internal val useArrayPolymorphism: Boolean = false,
    internal val classDiscriminator: String = defaultDiscriminator
) {

    @Suppress("DEPRECATION_ERROR")
    public fun copy(
        encodeDefaults: Boolean = true,
        ignoreUnknownKeys: Boolean = false,
        isLenient: Boolean = false,
        serializeSpecialFloatingPointValues: Boolean = false,
        allowStructuredMapKeys: Boolean = false,
        prettyPrint: Boolean = false,
        unquotedPrint: Boolean = false,
        indent: String = defaultIndent,
        coerceInputValues: Boolean = false,
        useArrayPolymorphism: Boolean = false,
        classDiscriminator: String = defaultDiscriminator
    ): JsonConfiguration {
        return JsonConfiguration(
            encodeDefaults,
            ignoreUnknownKeys,
            isLenient,
            serializeSpecialFloatingPointValues,
            allowStructuredMapKeys,
            prettyPrint,
            unquotedPrint,
            indent,
            coerceInputValues,
            useArrayPolymorphism,
            classDiscriminator
        )
    }

    init {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) require(indent == defaultIndent) {
            "Indent should not be specified when default printing mode is used"
        }

    }

    public companion object {
        @JvmStatic
        private val defaultIndent = "    "
        @JvmStatic
        private val defaultDiscriminator = "type"

        @JvmStatic
        public val Default: SubtypeToDetectDefault = SubtypeToDetectDefault()
        @JvmStatic
        public val Stable: SubtypeToDetectStable = SubtypeToDetectStable()
    }
}

/*
 * These types are introduced for migration purposes only and will be removed in 1.0.0 release
 */
@Suppress("DEPRECATION_ERROR")
public class SubtypeToDetectDefault : JsonConfiguration()
@Suppress("DEPRECATION_ERROR")
public class SubtypeToDetectStable : JsonConfiguration()

@Deprecated(
    level = DeprecationLevel.ERROR, message = "This named parameters were deprecated." +
            "Intstead of 'unquoted' please use 'unquotedPrint'. 'strictMode' was splitted into" +
            "'ignoreUnknownKeys', 'isLenient' and 'serializeSpecialFloatingPointValues'"
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED_PARAMETER")
@kotlin.internal.LowPriorityInOverloadResolution
public fun JsonConfiguration(strictMode: Boolean = true, unquoted: Boolean = false) {
    error("Should not be called")
}
