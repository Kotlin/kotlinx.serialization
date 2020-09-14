/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
@PublishedApi
internal annotation class SerializableWith(public val serializer: KClass<out KSerializer<*>>)
