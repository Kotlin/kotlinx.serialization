/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.native.concurrent.*

enum class Attitude { POSITIVE, NEUTRAL, NEGATIVE }

@Serializable
data class Tree(val name: String, val left: Tree? = null, val right: Tree? = null)

@Serializable
data class TypesUmbrella(
    val unit: Unit,
    val boolean: Boolean,
    val byte: Byte,
    val short: Short,
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val char: Char,
    val string: String,
    val enum: Attitude,
    val intData: IntData,
    val unitN: Unit?,
    val booleanN: Boolean?,
    val byteN: Byte?,
    val shortN: Short?,
    val intN: Int?,
    val longN: Long?,
    val floatN: Float?,
    val doubleN: Double?,
    val charN: Char?,
    val stringN: String?,
    val enumN: Attitude?,
    val intDataN: IntData?,
    val listInt: List<Int>,
    val listIntN: List<Int?>,
    val listNInt: Set<Int>?,
    val listNIntN: MutableSet<Int?>?,
    val listListEnumN: List<List<Attitude?>>,
    val listIntData: List<IntData>,
    val listIntDataN: MutableList<IntData?>,
    val tree: Tree,
    val mapStringInt: Map<String, Int>,
    val mapIntStringN: Map<Int, String?>,
    val arrays: ArraysUmbrella
)

@Serializable
data class ArraysUmbrella(
    val arrByte: Array<Byte>,
    val arrInt: Array<Int>,
    val arrIntN: Array<Int?>,
    val arrIntData: Array<IntData>
) {
    override fun equals(other: Any?) = other is ArraysUmbrella &&
            arrByte.contentEquals(other.arrByte) &&
            arrInt.contentEquals(other.arrInt) &&
            arrIntN.contentEquals(other.arrIntN) &&
            arrIntData.contentEquals(other.arrIntData)
}

@SharedImmutable
val umbrellaInstance = TypesUmbrella(
    Unit, true, 10, 20, 30, 40, 50.1f, 60.1, 'A', "Str0", Attitude.POSITIVE, IntData(70),
    null, null, 11, 21, 31, 41, 51.1f, 61.1, 'B', "Str1", Attitude.NEUTRAL, null,
    listOf(1, 2, 3),
    listOf(4, 5, null),
    setOf(6, 7, 8),
    mutableSetOf(null, 9, 10),
    listOf(listOf(Attitude.NEGATIVE, null)),
    listOf(IntData(1), IntData(2), IntData(3)),
    mutableListOf(IntData(1), null, IntData(3)),
    Tree("root", Tree("left"), Tree("right", Tree("right.left"), Tree("right.right"))),
    mapOf("one" to 1, "two" to 2, "three" to 3),
    mapOf(0 to null, 1 to "first", 2 to "second"),
    ArraysUmbrella(
        arrayOf(1, 2, 3),
        arrayOf(100, 200, 300),
        arrayOf(null, -1, -2),
        arrayOf(IntData(1), IntData(2))
    )
)
