package kotlinx.serialization

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
impl fun <T: Any> KClass<T>.serializer(): KSerializer<T> = this.js.asDynamic().Companion as KSerializer<T>

impl fun String.toUtf8Bytes(): ByteArray {
    val s = this
    val blck = js("unescape(encodeURIComponent(s))") // contains only chars that fit to byte
    return (blck as String).toList().map { it.toByte() }.toByteArray()
}

impl fun stringFromUtf8Bytes(bytes: ByteArray): String {
    val s = bytes.map { (it.toInt() and 0xFF).toChar() }.joinToString(separator = "") // wide uint8 to char
    val ans = js("decodeURIComponent(escape(s))")
    return ans as String
}

impl fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = enumClass.js.asDynamic().`valueOf_61zpoe$`(value) as E
impl fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = (enumClass.js.asDynamic().values() as Array<E>)[ordinal]

impl fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.js.name

impl fun <E: Any> ArrayList<E?>.toNativeArray(eClass: KClass<E>): Array<E?> = toTypedArray()