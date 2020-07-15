/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@Serializable
data class TestInt(@ProtoNumber(1) @ProtoType(ProtoIntegerType.SIGNED) val a: Int)

@Serializable
data class TestList(@ProtoNumber(1) val a: List<Int> = emptyList())

@Serializable
data class TestString(@ProtoNumber(2) val b: String)

@Serializable
data class TestInner(@ProtoNumber(3) val a: TestInt)

@Serializable
data class TestComplex(@ProtoNumber(42) val b: Int, @ProtoNumber(2) val c: String)

@Serializable
data class TestNumbers(@ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val a: Int, @ProtoNumber(2) val b: Long)

@Serializable
data class TestIntWithList(
    @ProtoNumber(1) val s: Int,
    @ProtoNumber(10) val l: List<Int>
)
