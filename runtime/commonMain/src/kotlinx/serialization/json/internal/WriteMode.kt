/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.InvalidKeyKindException
import kotlin.jvm.JvmField

internal enum class WriteMode(@JvmField val begin: Char, @JvmField val end: Char) {
    OBJ(BEGIN_OBJ, END_OBJ),
    LIST(BEGIN_LIST, END_LIST),
    MAP(BEGIN_OBJ, END_OBJ),
    POLY_OBJ(BEGIN_LIST, END_LIST);

    @JvmField
    val beginTc: Byte = charToTokenClass(begin)
    @JvmField
    val endTc: Byte = charToTokenClass(end)
}

internal fun Json.switchMode(desc: SerialDescriptor): WriteMode =
    when (desc.kind) {
        is PolymorphicKind -> WriteMode.POLY_OBJ
        StructureKind.LIST -> WriteMode.LIST
        StructureKind.MAP -> selectMapMode(desc, { WriteMode.MAP }, { WriteMode.LIST })
        else -> WriteMode.OBJ
    }

internal inline fun <T, R1 : T, R2 : T> Json.selectMapMode(
    mapDescriptor: SerialDescriptor,
    ifMap: () -> R1,
    ifList: () -> R2
): T {
    val keyDescriptor = mapDescriptor.getElementDescriptor(0)
    val keyKind = keyDescriptor.kind
    return if (keyKind is PrimitiveKind || keyKind == SerialKind.ENUM) {
        ifMap()
    } else if (configuration.allowStructuredMapKeys) {
        ifList()
    } else {
        throw InvalidKeyKindException(keyDescriptor)
    }
}
