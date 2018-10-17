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

import kotlinx.serialization.internal.defaultSerializer
import kotlin.reflect.KClass

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@UseExperimental(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class SharedImmutable()

fun <T : Any> KClass<T>.serializer(): KSerializer<T> = compiledSerializer() ?: defaultSerializer()
    ?: throw SerializationException("Can't locate argument-less serializer for $this. For generic classes, such as lists, please provide serializer explicitly.")

expect fun <T : Any> KClass<T>.compiledSerializer(): KSerializer<T>?

expect fun String.toUtf8Bytes(): ByteArray
expect fun stringFromUtf8Bytes(bytes: ByteArray): String

expect fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E
expect fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E

expect fun <E: Enum<E>> KClass<E>.enumClassName(): String
expect fun <E: Enum<E>> KClass<E>.enumMembers(): Array<E>

expect fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E>
