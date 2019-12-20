/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

@InternalSerializationApi
public interface GeneratedSerializer<T> : KSerializer<T> {
    fun childSerializers(): Array<KSerializer<*>>
}

/**
 * An internal interface used by the compiler plugin for objects that are factories of typed serializers, for example
 * for auto-generated companion objects for [Serializable] classes with type parameters.
 *
 * This interface is used to lookup and create serializers in K/N using `@AssociatedObjectKey`.
 * Should not be used in any user code. Please use generated `.serializer(kSerializer1, kSerializer2, ...)`
 * method on a companion or top-level `serializer(KType)` function.
 */
@InternalSerializationApi
@Deprecated("Inserted into generated code and should not be used directly", level = DeprecationLevel.HIDDEN)
public interface SerializerFactory {
    fun serializer(vararg typeParamsSerializers: KSerializer<*>): KSerializer<*>
}
