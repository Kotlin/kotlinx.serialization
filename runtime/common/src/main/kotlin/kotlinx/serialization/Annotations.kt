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

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Serializable(
    val with: KClass<out KSerializer<*>> = KSerializer::class // it means -- use default serializer by default
)

@Target(AnnotationTarget.CLASS)
annotation class Serializer(
    val forClass: KClass<*> // what class to create serializer for
)

// additional optional annotations

@Target(AnnotationTarget.PROPERTY)
annotation class SerialName(val value: String)

@Target(AnnotationTarget.PROPERTY)
annotation class Optional()

@Target(AnnotationTarget.PROPERTY)
annotation class Transient()

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo()
