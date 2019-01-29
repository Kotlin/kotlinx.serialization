/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import kotlin.reflect.KClass

/**
 * Instructs to use specific serializer for class or property.
 * If argument is omitted, plugin will generate default implementation inside the class.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Serializable(
    val with: KClass<out KSerializer<*>> = KSerializer::class // it means -- use default serializer by default
)

/**
 * Instructs plugin to turn this class into serializer for specified class [forClass].
 * However, it would not be used automatically. To apply it on particular class or property,
 * use [Serializable] or [UseSerializers], or [ContextualSerialization] with runtime registration.
 */
@Target(AnnotationTarget.CLASS)
annotation class Serializer(
    val forClass: KClass<*> // what class to create serializer for
)

/**
 * Overrides name visible to the runtime part of serialization framework
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class SerialName(val value: String)

/**
 * Indicates that property is optional in deserialization process.
 * Optional properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Optional

/**
 * Marks this property invisible for whole serialization framework.
 * Transient properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Transient

/**
 * When annotation class is marked with `@SerialInfo`, compiler plugin can instantiate it
 * and put into [SerialDescriptor], to be retrieved later during serialization process.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo

/**
 * Instructs to use [ContextSerializer] on an annotated property or type.
 * If used on a file, instructs to use [ContextSerializer] for all listed KClasses.
 *
 * @param [forClasses] Classes to use ContextSerializer for in current file.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE, AnnotationTarget.TYPE)
annotation class ContextualSerialization(vararg val forClasses: KClass<*>)

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
annotation class UseSerializers(vararg val serializerClasses: KClass<*>)

/**
 * Instructs to use [PolymorphicSerializer] on an annotated property or type.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
annotation class Polymorphic
