/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

@CoreFriendModuleApi
public fun isPartOfHierarchy(serializersModule: SerializersModule, value: Any?): Boolean {
    serializersModule as SerialModuleImpl
    return value != null && value::class in serializersModule.allImplementationsKClasses
}

