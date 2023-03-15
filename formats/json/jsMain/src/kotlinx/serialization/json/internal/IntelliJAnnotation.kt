/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.intellij.lang.annotations

import kotlinx.serialization.InternalSerializationApi

/**
 * JS implementation of JVM-only `org.intellij.lang.annotations.Language` class, adds syntax support by IDE.
 *
 * This class is missing from the Kotlin/JS targets, so it needs to be distributed along with the serialization runtime.
 *
 * Copy-paste from [https://github.com/JetBrains/java-annotations](https://github.com/JetBrains/java-annotations).
 *
 * @see [kotlinx.serialization.json.internal.FormatLanguage]
 */
@InternalSerializationApi
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS,
)
public annotation class Language(
    val value: String,
    val prefix: String = "",
    val suffix: String = "",
)