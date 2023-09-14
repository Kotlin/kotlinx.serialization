/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization.json.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.ElementMarker

@OptIn(ExperimentalSerializationApi::class)
internal class JsonElementMarker(descriptor: SerialDescriptor) {
    private val origin: ElementMarker = ElementMarker(descriptor, ::readIfAbsent)

    internal var isUnmarkedNull: Boolean = false
        private set

    internal fun mark(index: Int) {
        origin.mark(index)
    }

    internal fun nextUnmarkedIndex(): Int {
        return origin.nextUnmarkedIndex()
    }

    private fun readIfAbsent(descriptor: SerialDescriptor, index: Int): Boolean {
        isUnmarkedNull = !descriptor.isElementOptional(index) && descriptor.getElementDescriptor(index).isNullable
        return isUnmarkedNull
    }
}
