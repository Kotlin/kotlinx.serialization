/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING") // Parameters of annotations should probably be ignored, too

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlin.reflect.*

/**
 * Sets specific serializer for class, property or type argument.
 * When argument is omitted, plugin will generate default implementation inside the class.
 *
 * @see UseSerializers
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class Serializable(
    val with: KClass<out KSerializer<*>> = KSerializer::class // it means -- use default serializer by default
)

/**
 * Instructs plugin to turn this class into serializer for specified class [forClass].
 * However, it would not be used automatically. To apply it on particular class or property,
 * use [Serializable] or [UseSerializers], or [ContextualSerialization] with runtime registration.
 */
@Target(AnnotationTarget.CLASS)
public annotation class Serializer(
    val forClass: KClass<*> // what class to create serializer for
)

/**
 * Overrides name visible to the runtime part of serialization framework
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
public annotation class SerialName(val value: String)

/**
 * Indicates that property must be present during deserialization process,
 * even if it has default value.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class Required

/**
 * Marks this property invisible for whole serialization framework.
 * Transient properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class Transient

/**
 * When annotation class is marked with `@SerialInfo`, compiler plugin can instantiate it
 * and put into [SerialDescriptor], to be retrieved later during serialization process.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class SerialInfo

/**
 * Instructs to use [ContextSerializer] on an annotated property or type usage.
 * If used on a file, instructs to use [ContextSerializer] for all listed KClasses.
 *
 * @param [forClasses] Classes to use ContextSerializer for in current file.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE, AnnotationTarget.TYPE)
public annotation class ContextualSerialization(vararg val forClasses: KClass<*>)

/**
 *  Adds [serializerClasses] to serializers resolving process inside the plugin.
 *  Each of [serializerClasses] must implement [KSerializer].
 *
 *  Inside the file with this annotation, for each given property
 *  of type `T` in some serializable class, this list would be inspected for the presence of `KSerializer<T>`.
 *  If such serializer is present, it would be used instead of default.
 *
 *  Main use-case for this annotation is not to write @Serializable(with=SomeSerializer::class)
 *  on each property with custom serializer.
 *
 *  Serializers from this list have higher priority than default, but lesser priority than
 *  serializers defined on the property itself, such as [Serializable] (with=...) or [ContextualSerialization].
 */
@Target(AnnotationTarget.FILE)
public annotation class UseSerializers(vararg val serializerClasses: KClass<out KSerializer<*>>)

/**
 * Instructs to use [PolymorphicSerializer] on an annotated property or type usage.
 * When used on class, replaces its serializer with [PolymorphicSerializer] everywhere.
 *
 * This annotation is applied automatically to interfaces and serializable abstract classes
 * and can be applied to open classes in addition to [Serializable] for the sake of simplicity.
 *
 * Does not affect sealed classes, because they are gonna be serialized with subclasses automatically
 * with special compiler plugin support which would be added later.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
public annotation class Polymorphic

/**
 * Public API marked with this annotation is effectively **internal**, which means
 * it should not be used outside of `kotlinx.serialization`.
 * Signature, semantics, source and binary compatibilities are not guaranteed for this API
 * and will be changed without any warnings or migration aids.
 * If you cannot avoid using internal API to solve your problem, please report your use-case to serialization's issue tracker.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
public annotation class InternalSerializationApi

/**
 * Public API marked with this annotation is considered unstable and unsafe for general use.
 * Instability implies inconsistent behaviour across platforms, various edge-cases and use-cases that differ from very basic ones.
 * Unsafe API should not be generally used as the first-class mechanism and **may** be used as the last-ditch effort when every other
 * ways have failed.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
public annotation class UnsafeSerializationApi

/**
 * Marks declaration that obtains serializer implicitly using limited reflection capabilities.
 *
 * These declarations have the following limitations:
 * - Reflection cannot infer correct serializers for generic classes, like collections.
 * - Performance of reflective calls is usually worse than direct access to `.serializer`.
 *
 * It is recommended to specify serializer explicitly, using generated `.serializer()`
 * function on serializable class' companion.
 */
@RequiresOptIn
@Deprecated(level = DeprecationLevel.WARNING, message = "This annotation is obsolete and deprecated for removal")
public annotation class ImplicitReflectionSerializer

/**
 * This annotation marks declarations with default parameters that are subject to semantic change without a migration path.
 *
 * For example, [JsonConfiguration.Default] marked as unstable, thus indicating that it can change its default values
 * in the next releases (e.g. disable strict-mode by default), leading to a semantic (not source code level) change.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class UnstableDefault
