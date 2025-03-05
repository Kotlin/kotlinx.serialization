/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.tasks.*

val globalCompilerArgs
    get() = listOf(
        "-P", "plugin:org.jetbrains.kotlinx.serialization:disableIntrinsic=false",
        "-Xreport-all-warnings",
        "-Xrender-internal-diagnostic-names",
    )

val kotlin_Werror_override: String? by project

tasks.withType(KotlinCompilationTask::class).configureEach {
    compilerOptions {
        // Unconditional compiler options
        freeCompilerArgs.addAll(globalCompilerArgs)

        val isMainTaskName = name.startsWith("compileKotlin")
        if (isMainTaskName) {
            val werrorEnabled = when (kotlin_Werror_override?.lowercase()) {
                "disable" -> false
                "enable" -> true
                null -> true // Werror is enabled by default
                else -> throw GradleException("Invalid kotlin_Werror_override value. Use 'enable' or 'disable'")
            }

            allWarningsAsErrors = werrorEnabled

            // Add extra compiler options when -Werror is disabled
            if (!werrorEnabled) {
                freeCompilerArgs.addAll(
                    "-Wextra",
                    "-Xuse-fir-experimental-checkers"
                )
            }
        }
    }
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
}
tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
}
