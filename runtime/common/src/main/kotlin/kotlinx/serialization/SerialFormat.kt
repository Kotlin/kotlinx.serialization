package kotlinx.serialization

import kotlinx.serialization.context.*
import kotlinx.serialization.internal.HexConverter
import kotlin.reflect.KClass

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

interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T
}
