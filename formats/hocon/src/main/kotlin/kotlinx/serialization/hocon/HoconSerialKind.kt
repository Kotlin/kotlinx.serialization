package kotlinx.serialization.hocon

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.hoconKind(useArrayPolymorphism: Boolean): SerialKind = when (kind) {
    is PolymorphicKind -> {
        if (useArrayPolymorphism) StructureKind.LIST else StructureKind.MAP
    }
    else -> kind
}

@OptIn(ExperimentalSerializationApi::class)
internal val SerialKind.listLike
    get() = this == StructureKind.LIST || this is PolymorphicKind

@OptIn(ExperimentalSerializationApi::class)
internal val SerialKind.objLike
    get() = this == StructureKind.CLASS || this == StructureKind.OBJECT
