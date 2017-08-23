package kotlinx.serialization

import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

@Suppress("UNCHECKED_CAST")
impl fun <T: Any> KClass<T>.serializer(): KSerializer<T> = this.companionObjectInstance as KSerializer<T>

impl fun String.toUtf8Bytes() = this.toByteArray(Charsets.UTF_8)
impl fun stringFromUtf8Bytes(bytes: ByteArray) = String(bytes, Charsets.UTF_8)

impl fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = java.lang.Enum.valueOf(enumClass.java, value)
impl fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = enumClass.java.enumConstants[ordinal]

impl fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.qualifiedName ?: ""

@Suppress("UNCHECKED_CAST")
impl fun <E: Any> ArrayList<E?>.toNativeArray(eClass: KClass<E>): Array<E?> = toArray(java.lang.reflect.Array.newInstance(eClass.java, size) as Array<E?>)