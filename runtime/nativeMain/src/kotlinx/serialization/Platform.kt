/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*


actual fun String.toUtf8Bytes(): ByteArray {
    return this.toUtf8()
}

actual fun stringFromUtf8Bytes(bytes: ByteArray): String {
    return bytes.stringFromUtf8()
}


@UseExperimental(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
@Deprecated("Inserted into generated code and should not be used directly", level = DeprecationLevel.HIDDEN)
public annotation class SerializableWith(val serializer: KClass<out KSerializer<*>>)


@Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION_ERROR"
)
@UseExperimental(ExperimentalAssociatedObjects::class)
@ImplicitReflectionSerializer
actual fun <T : Any> KClass<T>.compiledSerializer(): KSerializer<T>? =
    findAssociatedObject<SerializableWith>() as? KSerializer<T>

actual fun <E : Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = TODO("Not supported in native")
actual fun <E : Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = TODO("Not supported in native")

actual fun <E : Enum<E>> KClass<E>.enumClassName(): String = this.simpleName ?: ""
actual fun <E : Enum<E>> KClass<E>.enumMembers(): Array<E> = TODO("Not supported in native")

actual fun <T : Any, E : T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> {
    val result = arrayOfAnyNulls<E>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    return result as Array<E>
}

@Suppress("UNCHECKED_CAST")
private fun <T> arrayOfAnyNulls(size: Int): Array<T> = arrayOfNulls<Any>(size) as Array<T>

internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.isInstance(this)

internal actual fun <T : Any> KClass<T>.simpleName(): String? = simpleName
