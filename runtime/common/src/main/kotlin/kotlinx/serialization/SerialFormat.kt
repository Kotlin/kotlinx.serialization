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

inline fun <reified T : Any> BinaryFormat.dump(obj: T): ByteArray = dump(context.getOrDefault(T::class), obj)
inline fun <reified T : Any> BinaryFormat.dumps(obj: T): String = HexConverter.printHexBinary(dump(obj), lowerCase = true)

inline fun <reified T : Any> BinaryFormat.load(raw: ByteArray): T = load(context.getOrDefault(T::class), raw)
inline fun <reified T : Any> BinaryFormat.loads(hex: String): T = load(HexConverter.parseHexBinary(hex))

interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T
}

inline fun <reified T : Any> StringFormat.stringify(obj: T): String = stringify(context.getOrDefault(T::class), obj)
inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String = stringify(context.getOrDefault(T::class).list, objects)
inline fun <reified K : Any, reified V: Any> StringFormat.stringify(map: Map<K, V>): String
        = stringify((context.getOrDefault(K::class) to context.getOrDefault(V::class)).map, map)

inline fun <reified T : Any> StringFormat.parse(str: String): T = parse(context.getOrDefault(T::class), str)
inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> = parse(context.getOrDefault(T::class).list, objects)
inline fun <reified K : Any, reified V: Any> StringFormat.parseMap(map: String)
        = parse((context.getOrDefault(K::class) to context.getOrDefault(V::class)).map, map)
