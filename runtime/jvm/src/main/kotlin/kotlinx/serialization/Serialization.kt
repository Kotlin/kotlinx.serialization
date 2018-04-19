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
actual fun <T: Any> KClass<T>.serializer(): KSerializer<T> = this.java.invokeSerializerGetter()
        ?: throw SerializationException("Can't locate default serializer for class $this")

actual fun String.toUtf8Bytes() = this.toByteArray(Charsets.UTF_8)
actual fun stringFromUtf8Bytes(bytes: ByteArray) = String(bytes, Charsets.UTF_8)

actual fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = java.lang.Enum.valueOf(enumClass.java, value)
actual fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = enumClass.java.enumConstants[ordinal]

actual fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.java.canonicalName ?: ""

@Suppress("UNCHECKED_CAST")
actual fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> = toArray(java.lang.reflect.Array.newInstance(eClass.java, size) as Array<E>)

@Suppress("UNCHECKED_CAST")
internal fun <T> Class<T>.invokeSerializerGetter(vararg args: KSerializer<Any>): KSerializer<T>? {
    val companion: Any = try {
        this.getField("Companion").apply { isAccessible = true }.get(null)
    } catch (e: NoSuchFieldException) {
        return null
    }
    return companion.javaClass.methods
            .find {it.name == "serializer" && it.parameterTypes.size == args.size && it.parameterTypes.all {it == KSerializer::class.java } }
            ?.invoke(companion, *args) as? KSerializer<T>
}
