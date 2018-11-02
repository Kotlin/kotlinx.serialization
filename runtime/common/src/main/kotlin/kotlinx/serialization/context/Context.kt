@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

internal typealias SerializersMap = MutableMap<KClass<*>, KSerializer<*>>

interface SerialContext {
    operator fun <T: Any> get(kclass: KClass<T>): KSerializer<T>?

    fun <T: Any> getByValue(value: T): KSerializer<T>?

    fun <T : Any> resolveFromBase(basePolyType: KClass<T>, concreteClass: KClass<out T>): KSerializer<out T>?

    fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>?
}

inline fun <reified T: Any> SerialContext.get(): KSerializer<T>? = get(T::class)

interface MutableSerialContext: SerialContext {
    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>)

    fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        basePolyType: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>
    )
}

class MutableSerialContextImpl(private val parentContext: SerialContext? = null): MutableSerialContext {

    private val classMap: SerializersMap = hashMapOf()

    private val polyMap: MutableMap<KClass<*>, SerializersMap> = hashMapOf()
    private val inverseClassNameMap: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()

    override fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>) {
        classMap[forClass] = serializer
    }

    override fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        basePolyType: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>
    ) {
        val name = concreteSerializer.descriptor.name
        polyMap.getOrPut(basePolyType, ::hashMapOf)[concreteClass] = concreteSerializer
        inverseClassNameMap.getOrPut(basePolyType, ::hashMapOf)[name] = concreteSerializer
    }

    override fun <T : Any> resolveFromBase(
        basePolyType: KClass<T>,
        concreteClass: KClass<out T>
    ): KSerializer<out T>? {
        return polyMap[basePolyType]?.get(concreteClass) as? KSerializer<out T>
    }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        return inverseClassNameMap[basePolyType]?.get(serializedClassName) as? KSerializer<out T>
    }

    override fun <T : Any> getByValue(value: T): KSerializer<T>? {
        val klass = value::class
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
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, concreteClass: KClass<out T>): KSerializer<out T>? =
        null

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        null
}
