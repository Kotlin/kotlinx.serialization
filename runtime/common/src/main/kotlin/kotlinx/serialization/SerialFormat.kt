package kotlinx.serialization

import kotlinx.serialization.context.*
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

// todo: better names
interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T
}
