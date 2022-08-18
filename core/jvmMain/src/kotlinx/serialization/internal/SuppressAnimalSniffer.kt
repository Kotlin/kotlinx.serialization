/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

/**
 * Suppresses Animal Sniffer plugin errors for certain classes.
 * Such classes are not available in Android API, but used only for JVM.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
internal annotation class SuppressAnimalSniffer
