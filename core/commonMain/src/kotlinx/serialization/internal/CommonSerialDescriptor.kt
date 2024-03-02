/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlin.reflect.*

internal abstract class CommonSerialDescriptor : SerialDescriptor {
    @ExperimentalSerializationApi
    override val useSerialPolymorphicNumbers: Boolean by lazy { super.useSerialPolymorphicNumbers }

    @ExperimentalSerializationApi
    override val serialPolymorphicNumberByBaseClass: Map<KClass<*>, Int> by lazy { super.serialPolymorphicNumberByBaseClass }
}