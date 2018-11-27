/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

enum class Platform {
    JVM, JS, NATIVE
}

public expect val currentPlatform: Platform

public fun isJs(): Boolean = currentPlatform == Platform.JS