// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats11

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any = list.removeFirst()
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)
}

fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}
