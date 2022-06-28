/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

internal enum class WriteMode(@JvmField val begin: Char, @JvmField val end: Char) {
    OBJ(BEGIN_OBJ, END_OBJ),
    LIST(BEGIN_LIST, END_LIST),
    MAP(BEGIN_OBJ, END_OBJ),
    POLY_OBJ(BEGIN_LIST, END_LIST);
}

@OptIn(ExperimentalSerializationApi::class)
internal fun Json.switchMode(desc: SerialDescriptor): WriteMode =
    when (desc.kind) {
        is PolymorphicKind -> WriteMode.POLY_OBJ
        StructureKind.LIST -> WriteMode.LIST
        StructureKind.MAP -> selectMapMode(desc, { WriteMode.MAP }, { WriteMode.LIST })
        else -> WriteMode.OBJ
    }

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <T, R1 : T, R2 : T> Json.selectMapMode(
    mapDescriptor: SerialDescriptor,
    ifMap: () -> R1,
    ifList: () -> R2
): T {
    val keyDescriptor = mapDescriptor.getElementDescriptor(0).carrierDescriptor(serializersModule)
    val keyKind = keyDescriptor.kind

    return if (keyKind is PrimitiveKind || keyKind == SerialKind.ENUM) {
        ifMap()
    } else if (configuration.allowStructuredMapKeys) {
        ifList()
    } else {
        throw InvalidKeyKindException(keyDescriptor)
    }
}

internal fun SerialDescriptor.carrierDescriptor(module: SerializersModule): SerialDescriptor = when {
    kind == SerialKind.CONTEXTUAL -> module.getContextualDescriptor(this)?.carrierDescriptor(module) ?: this
    isInline -> getElementDescriptor(0).carrierDescriptor(module)
    else     -> this
}
