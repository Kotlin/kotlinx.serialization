@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * Serial context is a runtime mechanism used by [ContextSerializer] and [PolymorphicSerializer]
 * to obtain serializers which were not found at compile-time by the serialization plugin.
 *
 * It can be regarded as a map where serializers are found using statically known KClasses.
 */
interface SerialContext {

    /**
     * Returns a dependent serializer associated with given [kclass].
     *
     * This method is used in context-sensitive operations
     * on a property marked with [ContextualSerialization], by a [ContextSerializer]
     */
    operator fun <T: Any> get(kclass: KClass<T>): KSerializer<T>?

    /**
     * Returns serializer registered for polymorphic serialization of an [obj]'s class in a scope of [basePolyType].
     *
     * This method is used inside a [PolymorphicSerializer] when statically known class of a property marked with [Polymorphic]
     * is [basePolyType], and the actual object in this property is [obj].
     */
    fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>?

    /**
     * Returns serializer registered for polymorphic serialization of a class with [serializedClassName] in a scope of [basePolyType].
     *
     * This method is used inside a [PolymorphicSerializer] when statically known class of a property marked with [Polymorphic]
     * is [basePolyType], and the class name received from [Decoder] is a [serializedClassName].
     */
    fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>?
}

inline fun <reified T: Any> SerialContext.get(): KSerializer<T>? = get(T::class)

/**
 * Returns a serializer associated with KClass which given [value] has.
 *
 * This method is used in context-sensitive operations
 * on a property marked with [ContextualSerialization], by a [ContextSerializer]
 */
fun <T: Any> SerialContext.getByValue(value: T): KSerializer<T>? {
    val klass = value::class
    return get(klass) as? KSerializer<T>
}

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getOrDefault(klass: KClass<T>) = this?.let { get(klass) } ?: klass.serializer()

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getByValueOrDefault(value: T): KSerializer<T> = this?.let { getByValue(value) } ?: value::class.serializer() as KSerializer<T>

/**
 * A [SerialContext] which always returns `null`.
 */
object EmptyContext: SerialContext {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? = null
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? =
        null

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        null
}

/**
 * A [SerialContext] which has ability to register a [KSerializer] in it.
 *
 * @see SerialModule
 */
interface MutableSerialContext: SerialContext {
    /**
     * Registers a [serializer] for a [forClass].
     *
     * Given [serializer] will be used in [get] and [getByValue].
     */
    fun <T: Any> registerSerializer(forClass: KClass<T>, serializer: KSerializer<T>)

    /**
     * Registers a [concreteSerializer] for a [concreteClass] in a scope of [basePolyType].
     *
     * Given [concreteSerializer] will be used in polymorphic serialization
     * if statically known type of property is [basePolyType] and runtime type is a [concreteClass].
     *
     * For serialization process, classname is determined using name of a serializer descriptor.
     *
     * All standard types and collections are registered in advance as a subtypes in a scope of [kotlin.Any].
     */
    fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        basePolyType: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>
    )
}

