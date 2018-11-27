package kotlinx.serialization

import kotlinx.serialization.context.*
import kotlinx.serialization.internal.HexConverter

interface SerialFormat {
    fun install(module: SerialModule)

    val context: SerialContext
}

abstract class AbstractSerialFormat: SerialFormat {
    protected val mutableContext: MutableSerialContext = MutableSerialContextImpl()

    override fun install(module: SerialModule) {
        module.registerIn(mutableContext)
    }

    override val context: SerialContext
        get() = mutableContext
}

interface BinaryFormat: SerialFormat {
    fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray
    fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, obj: T): String =
    HexConverter.printHexBinary(dump(serializer, obj), lowerCase = true)

fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, HexConverter.parseHexBinary(hex))

interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
