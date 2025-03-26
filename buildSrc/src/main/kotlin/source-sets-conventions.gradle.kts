/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("multiplatform")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

internal fun Project.versionCatalog(): VersionCatalog = versionCatalogs.named("libs")

kotlin {
    explicitApi()

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs.addAll("-Xjdk-release=1.8", "-Xjvm-default=all-compatibility")
        }
    }
    jvmToolchain(jdkToolchainVersion)

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_UMD
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    sourceSets.all {
        kotlin.srcDirs("$name/src")
        resources.srcDirs("$name/resources")
    }

    compilerOptions {
        // These configuration replaces 'languageSettings' config on line 67
        progressiveMode.set(true)
        optIn.addAll(
            listOf(
                "kotlin.ExperimentalMultiplatform",
                "kotlin.ExperimentalSubclassOptIn",
                "kotlinx.serialization.InternalSerializationApi",
                "kotlinx.serialization.SealedSerializationApi",
            )
        )
        if (overriddenLanguageVersion != null) {
            languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(versionCatalog().findLibrary("kotlin.stdlib").get())
            }
        }

        commonTest {
            dependencies {
                api(versionCatalog().findLibrary("kotlin.test").get())
            }
        }

        register("wasmMain") {
            dependsOn(commonMain.get())
        }
        register("wasmTest") {
            dependsOn(commonTest.get())
        }

        named("wasmJsMain") {
            dependsOn(named("wasmMain").get())
        }

        named("wasmJsTest") {
            dependsOn(named("wasmTest").get())
        }

        named("wasmWasiMain") {
            dependsOn(named("wasmMain").get())
        }

        named("wasmWasiTest") {
            dependsOn(named("wasmTest").get())
        }
    }

    sourceSets.matching({ it.name.contains("Test") }).configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.SealedSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}
