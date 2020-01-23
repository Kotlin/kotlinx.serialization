/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

private const val message =
    "Mapper was renamed to Properties to better reflect its semantics and extracted to separate artifact kotlinx-serialization-properties"

@Deprecated(message = message, level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("Properties"))
public class Mapper()
