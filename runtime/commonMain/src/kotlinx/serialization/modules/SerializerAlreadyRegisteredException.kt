/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * Thrown on attempt to register serializer (for contextual or polymorphic serialization)
 * for some class twice.
 *
 * Registering in the same module twice is prohibited. In case you are combining modules
 * with [SerialModule.plus], consider using [SerialModule.overwriteWith] if you want overwriting behaviour.
 */
@InternalSerializationApi // Will be hidden in the next release
public class SerializerAlreadyRegisteredException internal constructor(msg: String) : IllegalArgumentException(msg) {
    constructor(
        baseClass: KClass<*>,
        concreteClass: KClass<*>
    ) : this("Serializer for $concreteClass already registered in the scope of $baseClass")

    public constructor(forClass: KClass<*>) : this("Serializer for $forClass already registered in this module")
}
