/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "SerialModule was renamed to SerializersModule during serialization 1.0 API stabilization"
)
public typealias SerialModule = SerializersModule

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "EmptyModule was renamed to EmptySerializersModule during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("EmptySerializersModule")
)
public typealias EmptyModule = SerializersModule

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "SerialModuleCollector was renamed to SerializersModuleCollector during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("SerializersModuleCollector")
)
public typealias  SerialModuleCollector = SerializersModuleCollector