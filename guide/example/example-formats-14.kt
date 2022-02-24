// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats14

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

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }                                                

    override fun encodeNull() = encodeValue("NULL")
    override fun encodeNotNullMark() = encodeValue("!!")
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0
    
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"
}

fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User?, val votes: Int?)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin") , null)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}

