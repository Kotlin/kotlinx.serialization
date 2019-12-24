/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

sealed class SerialKind {
    // KNPE should never happen, because SerialKind is sealed and all inheritors are non-anonymous
    override fun toString(): String = this::class.simpleName()!!
}

sealed class PrimitiveKind : SerialKind() {
    object INT : PrimitiveKind()
    object UNIT : PrimitiveKind()
    object BOOLEAN : PrimitiveKind()
    object BYTE : PrimitiveKind()
    object SHORT : PrimitiveKind()
    object LONG : PrimitiveKind()
    object FLOAT : PrimitiveKind()
    object DOUBLE : PrimitiveKind()
    object CHAR : PrimitiveKind()
    object STRING : PrimitiveKind()
}

sealed class StructureKind : SerialKind() {
    object CLASS : StructureKind()
    object LIST : StructureKind()
    object MAP : StructureKind()
}

@Suppress("unused", "PropertyName")
sealed class UnionKind : SerialKind() {
    object OBJECT : UnionKind()
    object ENUM_KIND : UnionKind() // https://github.com/JetBrains/kotlin-native/issues/1447

    companion object {
        @Deprecated(
            "Moved out from UnionKind to simplify instance check for both POLYMORPHIC and SEALED. You can use 'is PolymorphicKind' now.",
            ReplaceWith("PolymorphicKind.OPEN"),
            DeprecationLevel.ERROR
        )
        val POLYMORPHIC = PolymorphicKind.OPEN
        @Deprecated(
            "Moved out from UnionKind to simplify instance check for both POLYMORPHIC and SEALED. You can use 'is PolymorphicKind' now.",
            ReplaceWith("PolymorphicKind.SEALED"),
            DeprecationLevel.ERROR
        )
        val SEALED = PolymorphicKind.SEALED
    }
}

sealed class PolymorphicKind : SerialKind() {
    object SEALED : PolymorphicKind()
    object OPEN : PolymorphicKind()
}

