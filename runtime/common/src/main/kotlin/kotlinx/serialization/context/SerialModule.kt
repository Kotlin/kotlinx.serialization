package kotlinx.serialization.context

import kotlinx.serialization.KSerializer
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
    private val modules: MutableList<SerialModule> = modules.toMutableList()

    override fun registerIn(context: MutableSerialContext) {
        modules.forEach { it.registerIn(context) }
    }

    operator fun plusAssign(module: SerialModule): Unit { modules += module }
    fun addModule(module: SerialModule) = plusAssign(module)
}
