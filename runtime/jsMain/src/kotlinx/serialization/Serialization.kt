/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.js.asDynamic().Companion?.serializer() as? KSerializer<T>

@Suppress("UNUSED_VARIABLE") // KT-23633
actual fun String.toUtf8Bytes(): ByteArray {
    val s = this
    val block = js("unescape(encodeURIComponent(s))") // contains only chars that fit to byte
    return (block as String).toList().map { it.toByte() }.toByteArray()
}

@Suppress("UNUSED_VARIABLE") // KT-23633
actual fun stringFromUtf8Bytes(bytes: ByteArray): String {
    val s = bytes.map { (it.toInt() and 0xFF).toChar() }.joinToString(separator = "") // wide uint8 to char
    val ans = js("decodeURIComponent(escape(s))")
    return ans as String
}

actual fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = enumClass.js.asDynamic().`valueOf_61zpoe$`(value) as E
actual fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = (enumClass.js.asDynamic().values() as Array<E>)[ordinal]

actual fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.js.name
actual fun <E: Enum<E>> KClass<E>.enumMembers(): Array<E> = (this.js.asDynamic().values() as Array<E>)

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> = toTypedArray()

internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.isInstance(this)

internal actual fun <T : Any> KClass<T>.simpleName(): String? = simpleName

internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? {
    throw NotImplementedError("This method is not supported for Kotlin/JS yet. Please provide serializer explicitly.")
}

internal actual fun isReferenceArray(type: KType, rootClass: KClass<Any>): Boolean {
    val typeParameters = type.arguments
    if (typeParameters.size != 1) return false
    val parameter = typeParameters.single()
    // Fun fact -- star projections pass this check
    val variance = parameter.variance ?: error("Star projections are forbidden: $type")
    if (parameter.type == null) error("Star projections are forbidden: $type")
    val prefix = if (variance == KVariance.IN || variance == KVariance.OUT)
        variance.toString().toLowerCase() + " " else ""
    val parameterName = prefix + parameter.type.toString()
    val expectedName = "Array<$parameterName>"
    if (type.toString() != expectedName) {
        return false
    }
    return true
}
