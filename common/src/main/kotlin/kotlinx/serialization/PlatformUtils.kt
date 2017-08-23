package kotlinx.serialization

import kotlin.reflect.KClass

header fun <T: Any> KClass<T>.serializer(): KSerializer<T>

header fun String.toUtf8Bytes(): ByteArray
header fun stringFromUtf8Bytes(bytes: ByteArray): String

header fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E
header fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E

header fun <E: Enum<E>> KClass<E>.enumClassName(): String

header fun <E: Any> ArrayList<E?>.toNativeArray(eClass: KClass<E>): Array<E?>