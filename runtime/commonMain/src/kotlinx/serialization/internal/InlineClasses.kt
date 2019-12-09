/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.elementDescriptors

@Suppress("EqualsOrHashCode")
open class InlineClassDescriptor(name: String, underlyingDescriptor: SerialDescriptor) : SerialClassDescImpl(name) {
    init {
        pushDescriptor(underlyingDescriptor)
    }

    // should it have special kind?

    override val isInline: Boolean
        get() = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // instance check is not very reliable, is it?
        if (other !is InlineClassDescriptor) return false
        if (name != other.name) return false
        if (elementDescriptors() != other.elementDescriptors()) return false
        return true
    }
}
