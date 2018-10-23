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

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
actual fun <T: Any> KClass<T>.compiledSerializer(): KSerializer<T>? = this.java.invokeSerializerGetter()

actual fun String.toUtf8Bytes() = this.toByteArray(Charsets.UTF_8)
actual fun stringFromUtf8Bytes(bytes: ByteArray) = String(bytes, Charsets.UTF_8)

actual fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = java.lang.Enum.valueOf(enumClass.java, value)
actual fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = enumClass.java.enumConstants[ordinal]

actual fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.java.canonicalName ?: ""
actual fun <E : Enum<E>> KClass<E>.enumMembers(): Array<E> = this.java.enumConstants

@Suppress("UNCHECKED_CAST")
actual fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> = toArray(java.lang.reflect.Array.newInstance(eClass.java, size) as Array<E>)

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
internal fun <T> Class<T>.invokeSerializerGetter(vararg args: KSerializer<Any>): KSerializer<T>? {
    var serializer: KSerializer<T>? = null

    // Search for serializer defined on companion object.
    val companion = declaredFields.singleOrNull { it.name == "Companion" }?.apply { isAccessible = true }?.get(null)
    if (companion != null) {
        serializer = companion.javaClass.methods
            .find { method ->
                method.name == "serializer" && method.parameterTypes.size == args.size && method.parameterTypes.all { it == KSerializer::class.java }
            }
            ?.invoke(companion, *args) as? KSerializer<T>
    }

    // Search for default serializer in case no serializer is defined on companion object.
    if (serializer == null) {
        serializer =
            declaredClasses.singleOrNull { it.simpleName == ("\$serializer") }
            ?.getField("INSTANCE")?.get(null) as? KSerializer<T>
    }

    return serializer
}

actual fun getSerialId(desc: SerialDescriptor, index: Int): Int? {
    return desc.getElementAnnotations(index).filterIsInstance<SerialId>().singleOrNull()?.id
}

actual fun getSerialTag(desc: SerialDescriptor, index: Int): String? = desc.getElementAnnotations(index).filterIsInstance<SerialTag>().singleOrNull()?.tag
