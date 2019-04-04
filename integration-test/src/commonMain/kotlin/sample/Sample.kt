/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

expect object Platform {
    val name: String
}

fun hello(): String = "Hello from ${Platform.name}"
