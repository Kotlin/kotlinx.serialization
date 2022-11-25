package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*


/**
 * Represents naming strategy — a transformer for serial names in a [Json] format.
 * Transformed serial names are used for both serialization and deserialization.
 * Actual transformation happens in the [serialNameForJson] function.
 * A naming strategy is always applied globally in the Json configuration builder
 * (see [JsonBuilder.namingStrategy]).
 * However, it is possible to apply additional filtering inside the transformer using the `descriptor` parameter in [serialNameForJson].
 *
 * Original serial names are never used after transformation, so they are ignored in a Json input.
 * If the original serial name is present in the Json input but transformed is not,
 * [MissingFieldException] still would be thrown. If one wants to preserve the original serial name for deserialization,
 * one should use the [JsonNames] annotation.
 *
 * ### Common pitfalls in conjunction with other Json features
 *
 * * Due to the nature of kotlinx.serialization framework, naming strategy transformation is applied to all properties regardless
 * of whether their serial name was taken from the property name or provided by @[SerialName] annotation.
 * Effectively it means one cannot avoid transformation by explicitly specifying the serial name.
 *
 * * Collision of the transformed name with any other (transformed) properties serial names or any alternative names
 * specified with [JsonNames] will lead to a deserialization exception.
 *
 * * Naming strategies do not transform serial names of the types used for the polymorphism, as they always should be specified explicitly.
 * Values from [JsonClassDiscriminator] or global [JsonBuilder.classDiscriminator] also are not altered.
 *
 * ### Controversy about using global naming strategies
 *
 * Global naming strategies have one key trait that makes them a debatable and controversial topic:
 * They are very implicit. It means that by looking only at the definition of the class,
 * it is impossible to say which names it will have in the serialized form.
 * As a consequence, naming strategies are not friendly to refactorings. Programmer renaming `myId` to `userId` may forget
 * to rename `my_id`, and vice versa. Generally, any tools one can imagine work poorly with global naming strategies:
 * Find Usages/Rename in IDE, full-text search by grep, etc. For them, the original name and the transformed are two different things;
 * changing one without the other may introduce bugs in many unexpected ways.
 * The lack of a single place of definition, the inability to use automated tools, and more error-prone code lead
 * to greater maintenance efforts for code with global naming strategies.
 * However, there are cases where usage of naming strategies is inevitable, such as interop with existing API or migrating a large codebase.
 * Therefore, one should carefully weigh the pros and cons before considering adding global naming strategies to an application.
 */
@ExperimentalSerializationApi
public fun interface JsonNamingStrategy {
    /**
     * Accepts an original [serialName] (defined by property name in the class or [SerialName] annotation) and returns
     * a transformed serial name which should be used for serialization and deserialization.
     *
     * Besides string manipulation operations, it is also possible to implement transformations that depend on the [descriptor]
     * and its element (defined by [elementIndex]) currently being serialized.
     * It is guaranteed that `descriptor.getElementName(elementIndex) == serialName`.
     * For example, one can choose different transformations depending on [SerialInfo]
     * annotations (see [SerialDescriptor.getElementAnnotations]) or element optionality (see [SerialDescriptor.isElementOptional]).
     *
     * Note that invocations of this function are cached for performance reasons.
     * Caching strategy is an implementation detail and shouldn't be assumed as a part of the public API contract, as it may be changed in future releases.
     * Therefore, it is essential for this function to be pure: it should not have any side effects, and it should
     * return the same String for a given [descriptor], [elementIndex], and [serialName], regardless of the number of invocations.
     */
    public fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String

    /**
     * Contains basic, ready to use naming strategies.
     */
    @ExperimentalSerializationApi
    public companion object Builtins {

        /**
         * A strategy that transforms serial names from camel case to snake case — lowercase characters with words separated by underscores.
         * The descriptor parameter is not used.
         *
         * It applies to every character following transformation rules:
         *
         * 1. If character `C` is in upper case, and the previous character exists, was not uppercase, and was not underscore, character `C` is transformed into underscore + c.lowercaseChar(): `_c`.
         * 2. If character `C` is in upper case but does not match other conditions from 1., it is transformed into lowercase: `c`. Thus, upper case acronyms like URL are transformed correctly.
         * 3. Otherwise, the character remains intact.
         *
         * **Note on cases:** Whether a character is in upper case is determined by the result of [Char.isUpperCase] function.
         * Lowercase transformation is performed by [Char.lowercaseChar], not by [Char.lowercase],
         * and therefore does not support one-to-many and many-to-one character mappings.
         * See the documentation of these functions for details.
         */
        @ExperimentalSerializationApi
        public val SnakeCase: JsonNamingStrategy = object : JsonNamingStrategy {
            override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String =
                buildString(serialName.length * 2) {
                    var previousWasUppercase = false
                    serialName.forEach { c ->
                        if (c.isUpperCase()) {
                            if (!previousWasUppercase && isNotEmpty() && last() != '_')
                                append('_')
                            previousWasUppercase = true
                            append(c.lowercaseChar())
                        } else {
                            previousWasUppercase = false
                            append(c)
                        }
                    }
                }

            override fun toString(): String = "kotlinx.serialization.json.JsonNamingStrategy.SnakeCase"
        }
    }
}
