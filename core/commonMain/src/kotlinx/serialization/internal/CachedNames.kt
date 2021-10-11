/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.internal

import kotlinx.serialization.descriptors.*

/**
 * Internal interface used as a marker for [SerialDescriptor] in order
 * to retrieve the set of all element names without allocations.
 * Used by our implementations as a performance optimization.
 * It's not an instance of [SerialDescriptor] to simplify implementation via delegation
 */
internal interface CachedNames {

    /**
     * A set of all names retrieved from [SerialDescriptor.getElementName]
     */
    public val serialNames: Set<String>
}
