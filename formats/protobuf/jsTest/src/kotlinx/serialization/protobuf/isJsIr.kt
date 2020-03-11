/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

// from https://github.com/JetBrains/kotlin/blob/569187a7516e2e5ab217158a3170d4beb0c5cb5a/js/js.translator/testData/_commonFiles/testUtils.kt#L3
actual fun isJsIr(): Boolean =
    !eval("(typeof Kotlin != \"undefined\" && typeof Kotlin.kotlin != \"undefined\")").unsafeCast<Boolean>()
