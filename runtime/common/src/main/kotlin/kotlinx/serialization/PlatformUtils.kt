/*
 * Copyright 2017 JetBrains s.r.o.
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

expect fun <T: Any> KClass<T>.serializer(): KSerializer<T>

expect fun String.toUtf8Bytes(): ByteArray
expect fun stringFromUtf8Bytes(bytes: ByteArray): String

expect fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E
expect fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E

expect fun <E: Enum<E>> KClass<E>.enumClassName(): String

expect fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E>