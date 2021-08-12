/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test


import kotlin.native.concurrent.SharedImmutable


@SharedImmutable
public actual val currentPlatform: Platform = Platform.NATIVE
