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

import kotlinx.serialization.internal.*

val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
    get() = LinkedHashMapSerializer(this.first, this.second)

fun String.Companion.serializer(): KSerializer<String> = StringSerializer
fun Byte.Companion.serializer(): KSerializer<Byte> = ByteSerializer
fun Short.Companion.serializer(): KSerializer<Short> = ShortSerializer
fun Int.Companion.serializer(): KSerializer<Int> = IntSerializer
fun Long.Companion.serializer(): KSerializer<Long> = LongSerializer
fun Double.Companion.serializer(): KSerializer<Double> = DoubleSerializer
