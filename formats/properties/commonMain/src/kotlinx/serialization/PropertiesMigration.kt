/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

@Deprecated(
    "Properties class was moved to kotlinx.serialization.properties package",
    ReplaceWith("Properties", "kotlinx.serialization.properties.Properties"),
    DeprecationLevel.ERROR
)
public typealias Properties = kotlinx.serialization.properties.Properties
