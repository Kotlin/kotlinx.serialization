/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.Serializable

@Serializable
data class Simple(val a: String)

@Serializable
data class SmallZoo(
        val str: String,
        val i: Int,
        val nullable: Double?,
        val list: List<String>,
        val map: Map<Int, Boolean>,
        val inner: Simple,
        val innersList: List<Simple>
)

@Serializable
data class NumberZoo(
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val boolean: Boolean,
        val char: Char
)
