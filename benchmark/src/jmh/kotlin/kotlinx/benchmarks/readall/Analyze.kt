/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.benchmarks.readall

import java.io.*
import kotlin.math.*

fun Double.r(): Int {
    val v = this * 100
    return v.roundToInt()
}

fun main() {
    for (i in 1..31) {
        val path = "/Users/qwwdfsad/workspace/kotlinx.serialization/benchmark/build/classes/java/jmh/kotlinx/benchmarks/fields"
        val old = File(path, "Fields$i.class").length()
        val new = File(path, "Fields${i}New2.class").length()
        val diff = new - old
        val ratio = (diff / old.toDouble()).r()
        val perField = diff / i
        println("Fields: $i, diff: $ratio% ($diff bytes), overhead per field: $perField bytes")
    }
}
