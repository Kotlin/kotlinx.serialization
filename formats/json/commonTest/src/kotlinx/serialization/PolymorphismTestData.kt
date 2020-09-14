/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
open class PolyBase(val id: Int) {
    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "PolyBase(id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PolyBase
        if (id != other.id) return false
        return true
    }

}

// TODO sandwwraith moving this class to the corresponding tests breaks runtime in unexpected ways
@Serializable
data class PolyDefault(val json: JsonElement) : PolyBase(-1)

@Serializable
data class PolyDerived(val s: String) : PolyBase(1)

val BaseAndDerivedModule = SerializersModule {
    polymorphic(PolyBase::class, PolyBase.serializer()) {
        subclass(PolyDerived.serializer())
    }
}
