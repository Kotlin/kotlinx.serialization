/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

@Deprecated(
    "Moved to kotlinx.serialization.builtins package",
    ReplaceWith("LongAsStringSerializer", "kotlinx.serialization.builtins.LongAsStringSerializer"),
    level = DeprecationLevel.ERROR
)
public typealias LongAsStringSerializer = kotlinx.serialization.builtins.LongAsStringSerializer
