@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

interface SerialContext {
    operator fun <T: Any> get(kclass: KClass<T>): KSerializer<T>?

    fun <T: Any> getByValue(value: T): KSerializer<T>?
}

inline fun <reified T: Any> SerialContext.get(): KSerializer<T>? = get(T::class)

interface MutableSerialContext: SerialContext {
    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>)
}

class MutableSerialContextImpl(private val parentContext: SerialContext? = null): MutableSerialContext {

    private val classMap: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()

    override fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap[forClass] = serializer
    }

    override fun <T : Any> getByValue(value: T): KSerializer<T>? {
//        if (value == null) throw SerializationException("Cannot determine class for value $value")
        val t: T = value
        val klass = t::class
        return get(klass) as? KSerializer<T>
    }

    override fun <T: Any> get(kclass: KClass<T>): KSerializer<T>? = classMap[kclass] as? KSerializer<T> ?: parentContext?.get(kclass)
}

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getOrDefault(klass: KClass<T>) = this?.let { get(klass) } ?: klass.serializer()

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getByValueOrDefault(value: T): KSerializer<T> = this?.let { getByValue(value) } ?: value::class.serializer() as KSerializer<T>

object EmptyContext: SerialContext {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? = null
    override fun <T : Any> getByValue(value: T): KSerializer<T>? = null
}
