/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.internal.CommonEnumSerializer

@Suppress("TestFunctionName")
inline fun <reified E: Enum<E>> CommonEnumSerializer(serialName: String): CommonEnumSerializer<E> =
    CommonEnumSerializer(serialName, enumValues(), emptyArray())
