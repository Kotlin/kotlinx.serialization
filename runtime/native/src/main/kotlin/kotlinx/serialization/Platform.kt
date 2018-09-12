/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization

import kotlin.reflect.KClass


actual fun String.toUtf8Bytes(): ByteArray {
    @Suppress("UNRESOLVED_REFERENCE")
    return this.toUtf8()
}

actual fun stringFromUtf8Bytes(bytes: ByteArray): String {
    @Suppress("UNRESOLVED_REFERENCE")
    return bytes.stringFromUtf8()
}


@Suppress("UNCHECKED_CAST")
actual fun <T : Any> KClass<T>.compiledSerializer(): KSerializer<T>? = TODO("Intrinsic to be implemented in compiler")

actual fun <E : Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = TODO("Not supported in native")
actual fun <E : Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = TODO("Not supported in native")

actual fun <E : Enum<E>> KClass<E>.enumClassName(): String = this.simpleName ?: ""
actual fun <E : Enum<E>> KClass<E>.enumMembers(): Array<E> = TODO("Not supported in native")

actual fun <T : Any, E : T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> = TODO("Not supported in native")

actual fun getSerialId(desc: KSerialClassDesc, index: Int): Int? {
    return index
}

actual fun getSerialTag(desc: KSerialClassDesc, index: Int): String? = index.toString()
