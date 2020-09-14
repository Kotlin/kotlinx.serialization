/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

@Deprecated(
    message = "Deprecated in the favour of Unit.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Unit.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public fun UnitSerializer(): KSerializer<Unit> = Unit.serializer()

@Deprecated(
    message = "Deprecated during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ListSerializer(this)")
)
public val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ListSerializer(this)

@Deprecated(
    message = "Deprecated during serialization 1.0 API stabilization",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("SetSerializer(this)")
)
public val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = SetSerializer(this)
