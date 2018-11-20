package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlin.jvm.*

internal enum class WriteMode(@JvmField val begin: Char, @JvmField val end: Char) {
    OBJ(BEGIN_OBJ, END_OBJ),
    LIST(BEGIN_LIST, END_LIST),
    MAP(BEGIN_OBJ, END_OBJ),
    POLY(BEGIN_LIST, END_LIST),
    ENTRY(INVALID, INVALID);

    val beginTc: Byte = charToTokenClass(begin)
    val endTc: Byte = charToTokenClass(end)
}

internal fun switchMode(desc: SerialDescriptor, typeParams: Array<out KSerializer<*>>): WriteMode =
    when (desc.kind) {
        UnionKind.POLYMORPHIC -> WriteMode.POLY
        StructureKind.LIST -> WriteMode.LIST
        StructureKind.MAP -> {
            val keyKind = typeParams[0].descriptor.kind
            if (keyKind is PrimitiveKind || keyKind == UnionKind.ENUM_KIND)
                WriteMode.MAP
            else WriteMode.LIST
        }
        else -> WriteMode.OBJ
    }
