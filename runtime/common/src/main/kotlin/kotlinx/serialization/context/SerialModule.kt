package kotlinx.serialization.context

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlin.reflect.KClass

interface SerialModule {
    fun registerIn(context: MutableSerialContext)
}

class SimpleModule<T: Any>(val kClass: KClass<T>, val kSerializer: KSerializer<T>): SerialModule {
    override fun registerIn(context: MutableSerialContext) {
        context.registerSerializer(kClass, kSerializer)
    }
}

@Suppress("UNCHECKED_CAST")
class MapModule(val map: Map<KClass<*>, KSerializer<*>>): SerialModule {
    override fun registerIn(context: MutableSerialContext) {
        map.forEach { (k, s) -> context.registerSerializer(k as KClass<Any>, s as KSerializer<Any>) }
    }
}

class CompositeModule(modules: List<SerialModule> = listOf()): SerialModule {
    constructor(vararg modules: SerialModule) : this(modules.toList())

    private val modules: MutableList<SerialModule> = modules.toMutableList()

    override fun registerIn(context: MutableSerialContext) {
        modules.forEach { it.registerIn(context) }
    }

    operator fun plusAssign(module: SerialModule): Unit { modules += module }
    fun addModule(module: SerialModule) = plusAssign(module)
}

class PolymorphicModule<Base : Any>(val baseClass: KClass<Base>, val baseSerializer: KSerializer<Base>? = null) : SerialModule {
    private val subclasses: MutableList<Pair<KClass<out Base>, KSerializer<out Base>>> = mutableListOf()
    override fun registerIn(context: MutableSerialContext) {
        if (baseSerializer != null) context.registerPolymorphicSerializer(baseClass, baseClass, baseSerializer)
        subclasses.forEach { (k, s) ->
            context.registerPolymorphicSerializer(
                baseClass,
                k as KClass<Base>,
                s as KSerializer<Base>
            )
        }
    }

    fun <T : Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    operator fun <T : Base> Pair<KClass<T>, KSerializer<T>>.unaryPlus() = addSubclass(this.first, this.second)
}

inline fun <Base : Any> SerialFormat.installPolymorphicModule(
    baseClass: KClass<Base>,
    baseSerializer: KSerializer<Base>? = null,
    builder: PolymorphicModule<Base>.() -> Unit = {}
) {
    val module = PolymorphicModule(baseClass, baseSerializer)
    module.builder()
    install(module)
}
