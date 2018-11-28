/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * A [SerialModule] which registers all its content for polymorphic serialization in the scope of [baseClass].
 * If [baseSerializer] is present, registers it as a serializer for [baseClass] (which is useful if base class is serializable).
 * Subclasses with its serializers can be added via [addSubclass] or [unaryPlus].
 *
 * @see PolymorphicSerializer
 * @see MutableSerialContext.registerPolymorphicSerializer
 */
@Suppress("UNCHECKED_CAST")
class PolymorphicModule<Base : Any>(val baseClass: KClass<Base>, val baseSerializer: KSerializer<Base>? = null) :
    SerialModule {
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
     * Adds a [serializer] to this module as a serializer for [subclass].
     * When this module will be registered in context, [serializer] would be registered for [subclass]
     * in the scope of [baseClass].
     */
    public fun <T : Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    /**
     * @see addSubclass
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T: Base> addSubclass() = addSubclass(T::class, T::class.serializer())


    /**
     * @see addSubclass
     */
    public operator fun <T : Base> Pair<KClass<T>, KSerializer<T>>.unaryPlus() = addSubclass(this.first, this.second)


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
public inline fun <Base : Any> SerialFormat.installPolymorphicModule(
    baseClass: KClass<Base>,
    baseSerializer: KSerializer<Base>? = null,
    builder: PolymorphicModule<Base>.() -> Unit = {}
) {
    val module = PolymorphicModule(baseClass, baseSerializer)
    module.builder()
    install(module)
}
