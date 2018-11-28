@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

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
