package kotlinx.serialization

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
impl fun <T: Any> KClass<T>.serializer(): KSerializer<T> = this.js.asDynamic().Companion as KSerializer<T>

impl fun String.toUtf8Bytes(): ByteArray = this.toList().map { it.toByte() }.toByteArray() //todo: normal conversion!
impl fun stringFromUtf8Bytes(bytes: ByteArray): String = bytes.map { it.toChar() }.joinToString(separator = "") // todo

impl fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = enumClass.js.asDynamic().`valueOf_61zpoe$`(value) as E
impl fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = (enumClass.js.asDynamic().values() as Array<E>)[ordinal]

impl fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.js.name

impl fun <E: Any> ArrayList<E?>.toNativeArray(eClass: KClass<E>): Array<E?> = toTypedArray()