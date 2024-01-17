/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*

public abstract class JsonBuilderBase internal constructor(configuration: JsonConfiguration, module: SerializersModule) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    public var encodeDefaults: Boolean = configuration.encodeDefaults

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     */
    @ExperimentalSerializationApi
    public var explicitNulls: Boolean = configuration.explicitNulls

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = configuration.ignoreUnknownKeys

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    public var allowStructuredMapKeys: Boolean = configuration.allowStructuredMapKeys

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *  `false` by default.
     */
    public var prettyPrint: Boolean = configuration.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode
     * 4 spaces by default.
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    @ExperimentalSerializationApi
    public var prettyPrintIndent: String = configuration.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values to the default property value (if exists) in the following cases:
     *   1. JSON value is `null` but the property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     *
     * `false` by default.
     */
    public var coerceInputValues: Boolean = configuration.coerceInputValues

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     *
     * This option can only be used if [classDiscriminatorMode] in a default [ClassDiscriminatorMode.POLYMORPHIC] state.
     */
    public var useArrayPolymorphism: Boolean = configuration.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    public var classDiscriminator: String = configuration.classDiscriminator


    /**
     * Defines which classes and objects should have class discriminator added to the output.
     * [ClassDiscriminatorMode.POLYMORPHIC] by default.
     *
     * Other modes are generally intended to produce JSON for consumption by third-party libraries,
     * therefore, this setting does not affect the deserialization process.
     */
    @ExperimentalSerializationApi
    public var classDiscriminatorMode: ClassDiscriminatorMode = configuration.classDiscriminatorMode

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     * `true` by default.
     */
    public var useAlternativeNames: Boolean = configuration.useAlternativeNames

    /**
     * Specifies [JsonNamingStrategy] that should be used for all properties in classes for serialization and deserialization.
     *
     * `null` by default.
     *
     * This strategy is applied for all entities that have [StructureKind.CLASS].
     */
    @ExperimentalSerializationApi
    public var namingStrategy: JsonNamingStrategy? = configuration.namingStrategy

    /**
     * Enables decoding enum values in a case-insensitive manner.
     * Encoding is not affected.
     *
     * This affects both enum serial names and alternative names (specified with the [JsonNames] annotation).
     * In the following example, string `[VALUE_A, VALUE_B]` will be printed:
     * ```
     * enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }
     *
     * @Serializable
     * data class Outer(val enums: List<E>)
     *
     * val j = Json { decodeEnumsCaseInsensitive = true }
     * println(j.decodeFromString<Outer>("""{"enums":["value_A", "alternative"]}""").enums)
     * ```
     *
     * If this feature is enabled,
     * it is no longer possible to decode enum values that have the same name in a lowercase form.
     * The following code will throw a serialization exception:
     *
     * ```
     * enum class BadEnum { Bad, BAD }
     * val j = Json { decodeEnumsCaseInsensitive = true }
     * j.decodeFromString<Box<BadEnum>>("""{"boxed":"bad"}""")
     * ```
     */
    @ExperimentalSerializationApi
    public var decodeEnumsCaseInsensitive: Boolean = configuration.decodeEnumsCaseInsensitive

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Json] instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    public var serializersModule: SerializersModule = module

    @OptIn(ExperimentalSerializationApi::class)
    internal fun validateBase() {
        if (useArrayPolymorphism) {
            require(classDiscriminator == defaultDiscriminator) {
                "Class discriminator should not be specified when array polymorphism is specified"
            }
            require(classDiscriminatorMode == ClassDiscriminatorMode.POLYMORPHIC) {
                "useArrayPolymorphism option can only be used if classDiscriminatorMode in a default POLYMORPHIC state."
            }
        }

        if (!prettyPrint) {
            require(prettyPrintIndent == defaultIndent) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != defaultIndent) {
            // Values allowed by JSON specification as whitespaces
            val allWhitespaces = prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had $prettyPrintIndent"
            }
        }
    }

    internal abstract fun build(): JsonConfiguration
}

private const val defaultIndent = "    "
private const val defaultDiscriminator = "type"
