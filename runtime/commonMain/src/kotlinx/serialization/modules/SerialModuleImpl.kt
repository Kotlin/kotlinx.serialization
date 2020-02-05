/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * A default implementation of [SerialModule]
 * which uses hash maps to store serializers associated with KClasses.
 */
internal class SerialModuleImpl(
    private val class2Serializer: Map<KClass<*>, KSerializer<*>>,
    private val polyBase2Serializers: Map<KClass<*>, Map<KClass<*>, KSerializer<*>>>,
    private val polyBase2NamedSerializers: Map<KClass<*>, Map<String, KSerializer<*>>>) : SerialModule {

    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): KSerializer<out T>? {
        if (!value.isInstanceOf(baseClass)) return null
        val custom = polyBase2Serializers[baseClass]?.get(value::class) as? KSerializer<out T>
        if (custom != null) return custom
        if (baseClass == Any::class) {
            val serializer = StandardSubtypesOfAny.getSubclassSerializer(value)
            return serializer as? KSerializer<out T>
        }
        return null
    }

    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        val standardPolymorphic =
            if (baseClass == Any::class) StandardSubtypesOfAny.getDefaultDeserializer(serializedClassName)
            else null

        if (standardPolymorphic != null) return standardPolymorphic as KSerializer<out T>
        return polyBase2NamedSerializers[baseClass]?.get(serializedClassName) as? KSerializer<out T>
    }

    override fun <T: Any> getContextual(kclass: KClass<T>): KSerializer<T>? = class2Serializer[kclass] as? KSerializer<T>

    override fun dumpTo(collector: SerialModuleCollector) {
        class2Serializer.forEach { (kclass, serial) ->
            collector.contextual(
                kclass as KClass<Any>,
                serial.cast()
            )
        }

        polyBase2Serializers.forEach { (baseClass, classMap) ->
            classMap.forEach { (actualClass, serializer) ->
                collector.polymorphic(
                    baseClass as KClass<Any>,
                    actualClass as KClass<Any>,
                    serializer.cast()
                )
            }
        }
    }
}
