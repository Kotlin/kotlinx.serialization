/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*

/**
 * Instructs to use specific serializer for class, property or type argument.
 *
 * If argument is omitted, plugin will generate default implementation inside the class.
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
 * Indicates that property is optional in deserialization process.
 * Optional properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
@Deprecated("All properties with default values are considered optional now", level = DeprecationLevel.ERROR)
public annotation class Optional

/**
 * Indicates that property must be present during deserialization process,
 * even if it has default value.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Required

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
