import java.util.Locale

/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

fun String.capitalizeCompat() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(locale = Locale.getDefault()) else it.toString()
}