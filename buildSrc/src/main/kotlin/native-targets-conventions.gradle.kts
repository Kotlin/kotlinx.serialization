import org.jetbrains.kotlin.gradle.*

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {

        // According to https://kotlinlang.org/docs/native-target-support.html
        // Tier 1
        this@kotlin.macosX64()
        this@kotlin.macosArm64()
        this@kotlin.iosSimulatorArm64()
        this@kotlin.iosX64()

        // Tier 2
        this@kotlin.linuxX64()
        this@kotlin.linuxArm64()
        this@kotlin.watchosSimulatorArm64()
        this@kotlin.watchosX64()
        this@kotlin.watchosArm32()
        this@kotlin.watchosArm64()
        this@kotlin.tvosSimulatorArm64()
        this@kotlin.tvosX64()
        this@kotlin.tvosArm64()
        this@kotlin.iosArm64()

        // Tier 3
        this@kotlin.mingwX64()
        // https://github.com/square/okio/issues/1242#issuecomment-1759357336
        if (doesNotDependOnOkio(project)) {
            this@kotlin.androidNativeArm32()
            this@kotlin.androidNativeArm64()
            this@kotlin.androidNativeX86()
            this@kotlin.androidNativeX64()
            this@kotlin.watchosDeviceArm64()

            // Deprecated, but not removed
            this@kotlin.linuxArm32Hfp()
        }
    }
}

fun doesNotDependOnOkio(project: Project): Boolean {
    return !project.name.contains("json-okio") && !project.name.contains("json-tests")
}