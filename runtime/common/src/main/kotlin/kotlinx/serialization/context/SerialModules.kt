@file:Suppress("RedundantVisibilityModifier")

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

    public operator fun plusAssign(module: SerialModule): Unit { modules += module }
    public fun addModule(module: SerialModule) = plusAssign(module)
}

operator fun SerialModule.plus(other: SerialModule): CompositeModule {
    if (this is CompositeModule) {
        this += other
        return this
    }
    return CompositeModule(this, other)
}

@Suppress("UNCHECKED_CAST")
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

    public fun <T : Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    operator fun <T : Base> Pair<KClass<T>, KSerializer<T>>.unaryPlus() = addSubclass(this.first, this.second)


    /**
     * Registers all subtypes of this module in a new module with a scope of [newPolyBase].
     *
     * If base type of this module had a serializer, registers it, too.
     *
     * @param newPolyBase A new base polymorphic type. Should be supertype of current [baseClass].
     * @param newPolyBaseSerializer Serializer for the new base type, if needed.
     * @return A new polymorphic module with subclasses from this and [newPolyBase] as basePolyType.
     */
    public fun <NewBase : Any> rebind(
        newPolyBase: KClass<NewBase>,
        newPolyBaseSerializer: KSerializer<NewBase>? = null
    ): PolymorphicModule<NewBase> {
        val newModule = PolymorphicModule(newPolyBase, newPolyBaseSerializer)
        baseSerializer?.let { newModule.addSubclass(baseClass as KClass<NewBase>, baseSerializer as KSerializer<NewBase>) }
        subclasses.forEach { (k, v) ->
            newModule.addSubclass(k as KClass<NewBase>, v as KSerializer<NewBase>)
        }
        return newModule
    }

    /**
     * Registers all subtypes of this module in a new module with a scope of [newPolyBase]
     * and returns a composite module of this module and new.
     *
     * If base type of this module had a serializer, registers it, too.
     *
     * @param newPolyBase A new base polymorphic type. Should be supertype of current [baseClass].
     * @param newPolyBaseSerializer Serializer for the new base type, if needed.
     * @return A composite of: new polymorphic module with subclasses from this and [newPolyBase] as basePolyType; current.
     */
    public fun <NewBase : Any> bind(
        newPolyBase: KClass<NewBase>,
        newPolyBaseSerializer: KSerializer<NewBase>? = null
    ): CompositeModule = rebind(newPolyBase, newPolyBaseSerializer) + this
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
