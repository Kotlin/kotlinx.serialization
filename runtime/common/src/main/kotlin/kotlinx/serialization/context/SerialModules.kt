@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass

/**
 * SerialModule is a collection of classes associated with its serializers,
 * postponed for runtime resolution. Its single purpose is to register all
 * serializers it has in the given [MutableSerialContext].
 *
 * Typically, one can create a module (using library implementation or anonymous class)
 * per file, or per package, to hold
 * all serializers together and then register it in some [AbstractSerialFormat].
 *
 * @see AbstractSerialFormat.install
 */
interface SerialModule {

    /**
     * Registers everything it has in the [context].
     *
     * @see MutableSerialContext.registerSerializer
     * @see MutableSerialContext.registerPolymorphicSerializer
     */
    fun registerIn(context: MutableSerialContext)
}

/**
 * A [SerialModule] which registers one class with one serializer for [ContextSerializer].
 *
 * @see MutableSerialContext.registerSerializer
 */
class SimpleModule<T: Any>(val kClass: KClass<T>, val kSerializer: KSerializer<T>): SerialModule {
    override fun registerIn(context: MutableSerialContext) {
        context.registerSerializer(kClass, kSerializer)
    }
}

/**
 * A [SerialModule] which registers multiple classes with its serializers for [ContextSerializer].
 */
@Suppress("UNCHECKED_CAST")
class MapModule(val map: Map<KClass<*>, KSerializer<*>>): SerialModule {
    override fun registerIn(context: MutableSerialContext) {
        map.forEach { (k, s) -> context.registerSerializer(k as KClass<Any>, s as KSerializer<Any>) }
    }
}

/**
 * A [SerialModule] for composing other modules
 *
 * Has convenient operator [plusAssign].
 *
 * @see SerialModule.plus
 */
class CompositeModule(modules: List<SerialModule> = listOf()): SerialModule {
    constructor(vararg modules: SerialModule) : this(modules.toList())

    private val modules: MutableList<SerialModule> = modules.toMutableList()

    override fun registerIn(context: MutableSerialContext) {
        modules.forEach { it.registerIn(context) }
    }

    public operator fun plusAssign(module: SerialModule): Unit { modules += module }
    public fun addModule(module: SerialModule) = plusAssign(module)
}

/**
 * Composes [this] module with [other].
 */
operator fun SerialModule.plus(other: SerialModule): CompositeModule {
    if (this is CompositeModule) {
        this += other
        return this
    }
    return CompositeModule(this, other)
}

/**
 * A [SerialModule] which registers all its content for polymorphic serialization in the scope of [baseClass].
 * If [baseSerializer] is present, registers it as a serializer for [baseClass] (which is useful if base class is serializable).
 * Subclasses with its serializers can be added via [addSubclass] or [unaryPlus].
 *
 * @see PolymorphicSerializer
 * @see MutableSerialContext.registerPolymorphicSerializer
 */
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

    /**
     * @see PolymorphicModule.unaryPlus
     */
    public fun <T : Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    /**
     * Adds a pair of class and serializer to a scope of [baseClass] in this module
     */
    operator fun <T : Base> Pair<KClass<T>, KSerializer<T>>.unaryPlus() = addSubclass(this.first, this.second)


    /**
     * Adds all subtypes of this module to a new module with a scope of [newPolyBase].
     *
     * If base type of this module had a serializer, adds it, too.
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
     * Adds all subtypes of this module to a new module with a scope of [newPolyBase]
     * and returns a composite module of this module and new.
     *
     * If base type of this module had a serializer, adds it, too.
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

/**
 * Convenient DSL for [PolymorphicModule]
 *
 * @see PolymorphicModule.unaryPlus
 */
inline fun <Base : Any> SerialFormat.installPolymorphicModule(
    baseClass: KClass<Base>,
    baseSerializer: KSerializer<Base>? = null,
    builder: PolymorphicModule<Base>.() -> Unit = {}
) {
    val module = PolymorphicModule(baseClass, baseSerializer)
    module.builder()
    install(module)
}
