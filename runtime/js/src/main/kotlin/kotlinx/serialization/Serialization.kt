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

@Suppress("UNCHECKED_CAST")
actual fun <T: Any> KClass<T>.serializer(): KSerializer<T> = this.js.asDynamic().Companion?.serializer() as? KSerializer<T>
        ?: throw SerializationException("Can't locate default serializer for class $this")

actual fun String.toUtf8Bytes(): ByteArray {
    val s = this
    val blck = js("unescape(encodeURIComponent(s))") // contains only chars that fit to byte
    return (blck as String).toList().map { it.toByte() }.toByteArray()
}

actual fun stringFromUtf8Bytes(bytes: ByteArray): String {
    val s = bytes.map { (it.toInt() and 0xFF).toChar() }.joinToString(separator = "") // wide uint8 to char
    val ans = js("decodeURIComponent(escape(s))")
    return ans as String
}

actual fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = enumClass.js.asDynamic().`valueOf_61zpoe$`(value) as E
actual fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = (enumClass.js.asDynamic().values() as Array<E>)[ordinal]

actual fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.js.name

actual fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> = toTypedArray()