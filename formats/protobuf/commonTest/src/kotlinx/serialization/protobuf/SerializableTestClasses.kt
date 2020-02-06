/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@Serializable
data class TestInt(@ProtoId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Int)

@Serializable
data class TestList(@ProtoId(1) val a: List<Int> = emptyList())

@Serializable
data class TestString(@ProtoId(2) val b: String)

@Serializable
data class TestInner(@ProtoId(3) val a: TestInt)

@Serializable
data class TestComplex(@ProtoId(42) val b: Int, @ProtoId(2) val c: String)

@Serializable
data class TestNumbers(@ProtoId(1) @ProtoType(ProtoNumberType.FIXED) val a: Int, @ProtoId(2) val b: Long)

@Serializable
data class TestIntWithList(
    @ProtoId(1) val s: Int,
    @ProtoId(10) val l: List<Int>
)
