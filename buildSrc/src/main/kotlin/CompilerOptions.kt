import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.collections.addAll

/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

val defaultCompilerArgs
    get() = listOf(
        "-P", "plugin:org.jetbrains.kotlinx.serialization:disableIntrinsic=false",
        "-Xreport-all-warnings",
        "-Xrender-internal-diagnostic-names",
        "-Xreturn-value-checker=full",
    )

fun KotlinCommonCompilerOptions.defaultOptions() {
    freeCompilerArgs.addAll(defaultCompilerArgs)
}

fun KotlinJvmCompilerOptions.jvmOptions() {
    jvmTarget = JvmTarget.JVM_1_8
    freeCompilerArgs.addAll("-Xjdk-release=1.8")
}

fun KotlinCommonCompilerOptions.languageVersion(overriddenLanguageVersion: String?) {
    if (overriddenLanguageVersion != null) {
        languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}