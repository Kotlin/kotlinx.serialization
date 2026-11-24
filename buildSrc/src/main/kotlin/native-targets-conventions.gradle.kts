import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import kotlin.KotlinVersion as StdlibKotlinVersion

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {

    }

    // According to https://kotlinlang.org/docs/native-target-support.html
    // Tier 1
    macosArm64()
    iosSimulatorArm64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    // Starting from 2.5.0, watchosArm32 target is no longer supported.
    // While there is a DSL to configure it, it won't let you configure the target.
    // To prevent build failures in configurations where the project is built with the fresh Kotlin version,
    // let's skip target's configuration.
    // TODO: remove the block completely after updating to Kotlin 2.5
    val languageVersion = overriddenLanguageVersion?.let(KotlinVersion::fromVersion) ?: KotlinVersion.DEFAULT
    if (languageVersion < KotlinVersion.KOTLIN_2_5) {
        @Suppress("DEPRECATION_ERROR", "DEPRECATION")
        watchosArm32()
    }
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()
    iosArm64()

    // Tier 3

    // The PE default stack reserve is 1 MB, vs. 8 MB on Unix targets; deeply
    // nested JSON tests overflow it.
    mingwX64 {
        binaries.withType<TestExecutable>().configureEach {
            linkerOpts("-Wl,--stack,8388608")
        }
    }
    iosX64()
    watchosDeviceArm64()

    // Deprecated
    // https://github.com/square/okio/issues/1242#issuecomment-1759357336
    if (doesNotDependOnOkio(project)) {
        // Deprecated for removal: see KT-86581
        // kotlin 2.5.0 - deprecated warning
        // kotlin 2.5.20 - deprecated error
        // kotlin 2.6.0 - removed
        // Starting from 2.5.20, androidNative targets can't be built, see KT-90218.
        // While there is a DSL to configure them, it won't let you build the targets.
        // To prevent build failures in configurations where the project is built with the fresh Kotlin version,
        // let's skip targets' configuration.
        // The check compares only major.minor.patch, so 2.5.20 dev and pre-release builds also skip the targets.
        // TODO: remove the block completely after updating to Kotlin 2.5.20
        val (major, minor, patch) = project.getKotlinPluginVersion().substringBefore('-').split('.').map(String::toInt)
        if (!StdlibKotlinVersion(major, minor, patch).isAtLeast(2, 5, 20)) {
            @Suppress("DEPRECATION", "DEPRECATION_ERROR")
            androidNativeArm32()
            @Suppress("DEPRECATION", "DEPRECATION_ERROR")
            androidNativeArm64()
            @Suppress("DEPRECATION", "DEPRECATION_ERROR")
            androidNativeX86()
            @Suppress("DEPRECATION", "DEPRECATION_ERROR")
            androidNativeX64()
        }

        // Deprecated, but not removed
        @Suppress("DEPRECATION")
        linuxArm32Hfp()
    }

    // Deprecated for removal: see KT-78660
    // timeline: unknown, for additional information see the ticket 
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    macosX64()
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    watchosX64()
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    tvosX64()

    // setup tests running in RELEASE mode
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.test(listOf(NativeBuildType.RELEASE))
    }
    targets.withType<KotlinNativeTargetWithTests<*>>().configureEach {
        testRuns.create("releaseTest") {
            setExecutionSourceFrom(binaries.getTest(NativeBuildType.RELEASE))
        }
    }
}

fun doesNotDependOnOkio(project: Project): Boolean {
    return !project.name.contains("json-okio") && !project.name.contains("json-tests")
}
