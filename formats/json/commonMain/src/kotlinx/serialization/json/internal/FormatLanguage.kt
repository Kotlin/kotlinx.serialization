package kotlinx.serialization.json.internal;

import kotlinx.serialization.InternalSerializationApi

/**
 * Multiplatform analogue of `org.intellij.lang.annotations.Language` annotation.
 *
 * An alias is used instead of class, because the actual class in the JVM will conflict with the class from the stdlib -
 * we want to avoid the situation with different classes having the same fully-qualified name.
 * [see](https://github.com/JetBrains/java-annotations/issues/34)
 *
 * Specifies that an element of the program represents a string that is a source code on a specified language.
 * Code editors may use this annotation to enable syntax highlighting, code completion and other features
 * inside the literals that assigned to the annotated variables, passed as arguments to the annotated parameters,
 * or returned from the annotated methods.
 * <p>
 * This annotation also could be used as a meta-annotation, to define derived annotations for convenience.
 * E.g. the following annotation could be defined to annotate the strings that represent Java methods:
 *
 * <pre>
 *   &#64;Language(value = "JAVA", prefix = "class X{", suffix = "}")
 *   &#64;interface JavaMethod {}
 * </pre>
 * <p>
 * Note that using the derived annotation as meta-annotation is not supported.
 * Meta-annotation works only one level deep.
 */

@InternalSerializationApi
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
public expect annotation class FormatLanguage(
    public val value: String,
    // default parameters are not used due to https://youtrack.jetbrains.com/issue/KT-25946/
    public val prefix: String,
    public val suffix: String,
)
