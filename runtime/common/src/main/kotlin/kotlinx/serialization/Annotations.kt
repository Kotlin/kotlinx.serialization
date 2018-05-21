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
 */
@Target(AnnotationTarget.CLASS)
annotation class Serializer(
    val forClass: KClass<*> // what class to create serializer for
)

/**
 * Overrides name visible to runtime part of serialization framework
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class SerialName(val value: String)

/**
 * Indicates that property is optional in deserialization process.
 * Optional properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Optional()

/**
 * Marks this property invisible for whole serialization framework.
 * Transient properties must have default values.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Transient()

/**
 * When annotation class is marked with `@SerialInfo`, compiler plugin can instantiate it
 * and put into [KSerialClassDesc], to be retrieved later during serialization process.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo()
