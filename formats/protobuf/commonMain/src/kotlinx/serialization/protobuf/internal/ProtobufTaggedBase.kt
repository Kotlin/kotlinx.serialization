/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlin.jvm.*

/*
 * In ProtoBuf spec, ids from 19000 to 19999 are reserved for protocol use,
 * thus we are leveraging it here and use 19_500 as a marker no one us allowed to use.
 * It does not leak to the resulting output.
 */
internal const val MISSING_TAG = 19_500L

internal abstract class ProtobufTaggedBase {
    private var tagsStack = LongArray(8)
    @JvmField
    protected var stackSize = -1

    protected val currentTag: ProtoDesc
        get() = tagsStack[stackSize]

    protected val currentTagOrDefault: ProtoDesc
        get() = if (stackSize == -1) MISSING_TAG else tagsStack[stackSize]

    protected fun popTagOrDefault(): ProtoDesc = if (stackSize == -1) MISSING_TAG else tagsStack[stackSize--]

    protected fun pushTag(tag: ProtoDesc) {
        if (tag == MISSING_TAG) return // Missing tag is never added
        val idx = ++stackSize
        if (stackSize >= tagsStack.size) {
            expand()
        }
        tagsStack[idx] = tag
    }

    private fun expand() {
        tagsStack = tagsStack.copyOf(tagsStack.size * 2)
    }

    protected fun popTag(): ProtoDesc {
        if (stackSize >= 0) return tagsStack[stackSize--]
        // Unreachable
        throw SerializationException("No tag in stack for requested element")
    }

    protected inline fun <E> tagBlock(tag: ProtoDesc, block: () -> E): E {
        pushTag(tag)
        return block()
    }
}
