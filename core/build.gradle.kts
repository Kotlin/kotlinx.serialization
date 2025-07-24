import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.targets.js.ir.*

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

kotlin {
    sourceSets {
        jvmTest {
            dependencies {
                implementation(libs.kotlintest)
                implementation(libs.guava.get24())
                implementation(libs.gson)
                implementation(libs.coroutines.core)
            }
        }
    }
}

/*
 These manifest values help kotlinx.serialization compiler plugin determine if it is compatible with a given runtime library.
 Plugin reads them during compilation.

 Implementation-Version is used to determine whether runtime library supports a given plugin feature (e.g. value classes serialization
 in Kotlin 1.x may require runtime library version 1.y to work).
 Compiler plugin may enable or disable features by looking at Implementation-Version.

 Require-Kotlin-Version is used to determine whether runtime library with new features can work with old compilers.
 In ideal case, its value should always be 1.4, but some refactorings (e.g. adding a method to the Encoder interface)
 may unexpectedly break old compilers, so it is left out as a safety net. Compiler plugins, starting from 1.4 are instructed
 to reject runtime if runtime's Require-Kotlin-Version is greater than the current compiler.
 */
tasks.withType<Jar>().named(kotlin.jvm().artifactsTaskName) {

    // adding the ProGuard rules to the jar
    from(rootDir.resolve("rules/common.pro")) {
        rename { "kotlinx-serialization-common.pro" }
        into("META-INF/proguard")
    }
    from(rootDir.resolve("rules/common.pro")) {
        rename { "kotlinx-serialization-common.pro" }
        into("META-INF/com.android.tools/proguard")
    }
    from(rootDir.resolve("rules/common.pro")) {
        rename { "kotlinx-serialization-common.pro" }
        into("META-INF/com.android.tools/r8")
    }
    from(rootDir.resolve("rules/r8.pro")) {
        rename { "kotlinx-serialization-r8.pro" }
        into("META-INF/com.android.tools/r8")
    }


    manifest {
        attributes(
                "Implementation-Version" to version,
                "Require-Kotlin-Version" to "2.0.0-RC1",
        )
    }
}

configureJava9ModuleInfo()

tasks.withType<KotlinJsIrLink>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
}
