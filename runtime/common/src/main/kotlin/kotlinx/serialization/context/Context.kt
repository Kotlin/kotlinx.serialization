@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.KClass

internal typealias SerializersMap = MutableMap<KClass<*>, KSerializer<*>>

interface SerialContext {
    operator fun <T: Any> get(kclass: KClass<T>): KSerializer<T>?

    fun <T: Any> getByValue(value: T): KSerializer<T>?

    fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>?

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

    @ImplicitReflectionSerializer
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? {
        if (!basePolyType.isInstance(obj)) return null
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
    @ImplicitReflectionSerializer
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

    @UseExperimental(ImplicitReflectionSerializer::class)
    private val deserializingMap: Map<String, KSerializer<*>> = map.mapKeys { (_, s) -> s.descriptor.name }

    @Suppress("UNCHECKED_CAST")
    @ImplicitReflectionSerializer
    internal fun getSubclassSerializer(objectToCheck: Any): KSerializer<*>? {
        // todo: arrays?
        for ((k, v) in map) {
            if (k.isInstance(objectToCheck)) return v
        }
        return null
    }

    internal fun getDefaultDeserializer(serializedClassName: String): KSerializer<*>? = deserializingMap[serializedClassName]
}

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getOrDefault(klass: KClass<T>) = this?.let { get(klass) } ?: klass.serializer()

@ImplicitReflectionSerializer
fun <T: Any> SerialContext?.getByValueOrDefault(value: T): KSerializer<T> = this?.let { getByValue(value) } ?: value::class.serializer() as KSerializer<T>

object EmptyContext: SerialContext {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? = null
    override fun <T : Any> getByValue(value: T): KSerializer<T>? = null
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? =
        null

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        null
}
