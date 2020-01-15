/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.internal.EnumSerializer

@Suppress("TestFunctionName")
inline fun <reified E : Enum<E>> EnumSerializer(serialName: String): EnumSerializer<E> =
    EnumSerializer(serialName, enumValues())
