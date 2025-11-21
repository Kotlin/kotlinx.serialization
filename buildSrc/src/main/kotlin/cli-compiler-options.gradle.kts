/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.tasks.*
import kotlin.collections.joinToString

/**
 * This file is intended for compiler options that affect CLI compilation only.
 * The reason is that tasks.withType(...) affects only platform compilations,
 * so you may get incorrect analysis results in IDE on 'intermediate' source sets.
 *
 * If compiler option is likely to affect IDE (e.g., new diagnostic or language feature),
 * add it to CompilerOptions.kt instead.
 */

// Used only for User Projects TeamCity configurations, no IDE there
val kotlinAdditionalCliOptions = providers.gradleProperty("kotlin_additional_cli_options")
    .orNull?.let { options ->
        options.removeSurrounding("\"").split(" ").filter { it.isNotBlank() }
    }

val kotlin_Werror_override: String? by project

// -Werror option only for test source sets
// Cannot migrate to general compilerOptions {} because we need compilation task name
// to know whether it is main or test SS.
tasks.withType(KotlinCompilationTask::class).configureEach {
    compilerOptions {
        kotlinAdditionalCliOptions?.forEach { option -> freeCompilerArgs.add(option) }

        val isMainTaskName = name.startsWith("compileKotlin")
        if (isMainTaskName) {
            val werrorEnabled = when (kotlin_Werror_override?.lowercase()) {
                "disable" -> false
                "enable" -> true
                null -> true // Werror is enabled by default
                else -> throw GradleException("Invalid kotlin_Werror_override value. Use 'enable' or 'disable'")
            }
            allWarningsAsErrors = werrorEnabled
        }
    }
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
}
tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    doFirst {
        logger.info("Added Kotlin compiler flags: ${compilerOptions.freeCompilerArgs.get().joinToString(", ")}")
        logger.info("allWarningsAsErrors=${compilerOptions.allWarningsAsErrors.get()}")
    }
}
