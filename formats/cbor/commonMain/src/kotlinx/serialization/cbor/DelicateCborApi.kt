/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY) // no direct targets, only argument to @SubclassOptInRequired
@RequiresOptIn(message = "Accessing this property exposes mutable state. Read-Only access is fine, but manipulating it can cause undefined behaviour", level = RequiresOptIn.Level.ERROR)
public annotation class DelicateCborApi
