/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal;

import kotlinx.serialization.InternalSerializationApi

@InternalSerializationApi
public actual annotation class FormatLanguage(
    public actual val value: String,
    // default parameters are not used due to https://youtrack.jetbrains.com/issue/KT-25946/
    public actual val prefix: String,
    public actual val suffix: String,
)