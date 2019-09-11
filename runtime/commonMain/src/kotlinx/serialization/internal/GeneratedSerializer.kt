/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

@InternalSerializationApi
public interface GeneratedSerializer<T> : KSerializer<T> {
    fun childSerializers(): Array<KSerializer<*>>
}
