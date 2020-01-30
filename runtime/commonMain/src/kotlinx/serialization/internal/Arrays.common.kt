/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

// Array.get that checks indices on JS
internal expect fun <T> Array<T>.getChecked(index: Int): T
internal expect fun BooleanArray.getChecked(index: Int): Boolean
