@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.KClass

private typealias SerializersMap = MutableMap<KClass<*>, KSerializer<*>>

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
     * Returns a serializer associated with KClass which given [value] has.
     *
     * This method is used in context-sensitive operations
     * on a property marked with [ContextualSerialization], by a [ContextSerializer]
     */
    fun <T: Any> getByValue(value: T): KSerializer<T>?

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

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? {
        if (!isInstance(basePolyType, obj)) return null
        (if (basePolyType == Any::class) StandardSubtypesOfAny.getSubclassSerializer(obj) else null)?.let { return it as KSerializer<out T> }
        return polyMap[basePolyType]?.get(obj::class) as? KSerializer<out T>
    }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        (if (basePolyType == Any::class) StandardSubtypesOfAny.getDefaultDeserializer(serializedClassName) else null)?.let { return it as KSerializer<out T> }
        return inverseClassNameMap[basePolyType]?.get(serializedClassName) as? KSerializer<out T>
    }

    override fun <T : Any> getByValue(value: T): KSerializer<T>? {
        val klass = value::class
        return get(klass) as? KSerializer<T>
    }

    override fun <T: Any> get(kclass: KClass<T>): KSerializer<T>? = classMap[kclass] as? KSerializer<T> ?: parentContext?.get(kclass)
}


internal object StandardSubtypesOfAny {
    private val map: Map<KClass<*>, KSerializer<*>> = mapOf(
        List::class to ArrayListSerializer(makeNullable(PolymorphicSerializer(Any::class))),
        LinkedHashSet::class to LinkedHashSetSerializer(makeNullable(PolymorphicSerializer(Any::class))),
        HashSet::class to HashSetSerializer(makeNullable(PolymorphicSerializer(Any::class))),
        Set::class to LinkedHashSetSerializer(makeNullable(PolymorphicSerializer(Any::class))),
        LinkedHashMap::class to LinkedHashMapSerializer(makeNullable(PolymorphicSerializer(Any::class)), makeNullable(PolymorphicSerializer(Any::class))),
        HashMap::class to HashMapSerializer(makeNullable(PolymorphicSerializer(Any::class)), makeNullable(PolymorphicSerializer(Any::class))),
        Map::class to LinkedHashMapSerializer(makeNullable(PolymorphicSerializer(Any::class)), makeNullable(PolymorphicSerializer(Any::class))),
        Map.Entry::class to MapEntrySerializer(makeNullable(PolymorphicSerializer(Any::class)), makeNullable(PolymorphicSerializer(Any::class))),
        String::class to StringSerializer,
        Char::class to CharSerializer,
        Double::class to DoubleSerializer,
        Float::class to FloatSerializer,
        Long::class to LongSerializer,
        Int::class to IntSerializer,
        Short::class to ShortSerializer,
        Byte::class to ByteSerializer,
        Boolean::class to BooleanSerializer,
        Unit::class to UnitSerializer
    )

    private val deserializingMap: Map<String, KSerializer<*>> = map.mapKeys { (_, s) -> s.descriptor.name }

    @Suppress("UNCHECKED_CAST")
    internal fun getSubclassSerializer(objectToCheck: Any): KSerializer<*>? {
        for ((k, v) in map) {
            if (isInstance(k, objectToCheck)) return v
        }
        return null
    }

    internal fun getDefaultDeserializer(serializedClassName: String): KSerializer<*>? = deserializingMap[serializedClassName]
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
    override fun <T : Any> getByValue(value: T): KSerializer<T>? = null
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? =
        null

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        null
}
