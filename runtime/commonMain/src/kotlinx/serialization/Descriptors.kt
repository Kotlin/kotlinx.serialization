/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.PrimitiveDescriptor

sealed class SerialKind {
    override fun toString(): String =
        this::class.simpleName()!! // KNPE should never happen, because SerialKind is sealed and all inheritors are non-anonymous
}

sealed class PrimitiveKind: SerialKind() {
    object INT : PrimitiveKind()
    object UNIT : PrimitiveKind()
    object BOOLEAN : PrimitiveKind()
    object BYTE : PrimitiveKind()
    object SHORT : PrimitiveKind()
    object LONG : PrimitiveKind()
    object FLOAT : PrimitiveKind()
    object DOUBLE : PrimitiveKind()
    object CHAR : PrimitiveKind()
    object STRING: PrimitiveKind()
}

sealed class StructureKind: SerialKind() {
    object CLASS: StructureKind()
    object LIST: StructureKind()
    object MAP: StructureKind()
}

sealed class UnionKind: SerialKind() {
    object OBJECT: UnionKind()
    object ENUM_KIND: UnionKind() // https://github.com/JetBrains/kotlin-native/issues/1447
    object SEALED: UnionKind()
    object POLYMORPHIC: UnionKind()
}

class PrimitiveDescriptorWithName(override val name: String, val original: PrimitiveDescriptor) :
    SerialDescriptor by original

fun PrimitiveDescriptor.withName(name: String): SerialDescriptor = PrimitiveDescriptorWithName(name, this)
