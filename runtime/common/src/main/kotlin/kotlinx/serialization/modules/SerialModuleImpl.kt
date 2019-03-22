/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.modules

import kotlinx.serialization.KSerializer
import kotlinx.serialization.isInstanceOf
import kotlin.collections.set
import kotlin.reflect.KClass

private typealias SerializersMap = MutableMap<KClass<*>, KSerializer<*>>

/**
 * A default implementation of [SerialModule]
 * which uses hash maps to store serializers associated with KClasses.
 */
internal class SerialModuleImpl : SerialModule {

    private val classMap: SerializersMap = hashMapOf()

    private val polyMap: MutableMap<KClass<*>, SerializersMap> = hashMapOf()
    private val inverseClassNameMap: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()

    internal fun <T : Any> registerSerializer(
        forClass: KClass<T>,
        serializer: KSerializer<T>,
        allowOverwrite: Boolean = false
    ) {
        if (!allowOverwrite && forClass in classMap) throw SerializerAlreadyRegisteredException(forClass)
        classMap[forClass] = serializer
    }

    internal fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        baseClass: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>,
        allowOverwrite: Boolean = false
    ) {
        val name = concreteSerializer.descriptor.name
        polyMap.getOrPut(baseClass, ::hashMapOf).let { baseClassMap ->
            if (!allowOverwrite && concreteClass in baseClassMap) throw SerializerAlreadyRegisteredException(
                baseClass,
                concreteClass
            )
            baseClassMap[concreteClass] = concreteSerializer
        }
        inverseClassNameMap.getOrPut(baseClass, ::hashMapOf)[name] = concreteSerializer
    }

    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): KSerializer<out T>? {
        if (!value.isInstanceOf(baseClass)) return null
        (if (baseClass == Any::class) StandardSubtypesOfAny.getSubclassSerializer(value) else null)?.let { return it as KSerializer<out T> }
        return polyMap[baseClass]?.get(value::class) as? KSerializer<out T>
    }

    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        (if (baseClass == Any::class) StandardSubtypesOfAny.getDefaultDeserializer(
            serializedClassName
        ) else null)?.let { return it as KSerializer<out T> }
        return inverseClassNameMap[baseClass]?.get(serializedClassName) as? KSerializer<out T>
    }

    override fun <T: Any> getContextual(kclass: KClass<T>): KSerializer<T>? = classMap[kclass] as? KSerializer<T>

    override fun dumpTo(collector: SerialModuleCollector) {
        classMap.forEach { (kclass, serial) ->
            collector.contextual(
                kclass as KClass<Any>,
                serial as KSerializer<Any>
            )
        }

        polyMap.forEach { (baseClass, classMap) ->
            classMap.forEach { (actualClass, serializer) ->
                collector.polymorphic(
                    baseClass as KClass<Any>,
                    actualClass as KClass<Any>,
                    serializer as KSerializer<Any>
                )
            }
        }
    }
}
