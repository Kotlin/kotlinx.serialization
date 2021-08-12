/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

enum class Platform {
    JVM, JS_LEGACY, JS_IR, NATIVE
}

public expect val currentPlatform: Platform

public fun isJs(): Boolean = currentPlatform == Platform.JS_LEGACY || currentPlatform == Platform.JS_IR
public fun isJsLegacy(): Boolean = currentPlatform == Platform.JS_LEGACY
public fun isJsIr(): Boolean = currentPlatform == Platform.JS_IR
public fun isJvm(): Boolean = currentPlatform == Platform.JVM
public fun isNative(): Boolean = currentPlatform == Platform.NATIVE
