/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.SerializersModule

@Serializable
open class PolyBase(@SerialId(1) val id: Int) {
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

@Serializable
data class PolyDerived(@SerialId(2) val s: String) : PolyBase(1)

val BaseAndDerivedModule = SerializersModule {
    polymorphic(PolyBase::class, PolyBase.serializer()) {
        PolyDerived::class with PolyDerived.serializer()
    }
}

@Serializable
abstract class SimpleAbstract

@Serializable
data class SimpleIntInheritor(val i: Int, val s: String) : SimpleAbstract()

@Serializable
data class SimpleStringInheritor(val s: String, val i: Int) : SimpleAbstract()

@Serializable
data class PolyBox(@Polymorphic val boxed: SimpleAbstract)

val SimplePolymorphicModule = SerializersModule {
    polymorphic<SimpleAbstract> {
        SimpleIntInheritor::class with SimpleIntInheritor.serializer()
        SimpleStringInheritor::class with SimpleStringInheritor.serializer()
    }
}

@Serializable
data class SealedBox(val boxed: List<SimpleSealed>)
