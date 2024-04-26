/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.testing.*

plugins {
    kotlin("multiplatform")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

// Unfortunately there is no compatible version of okio for Wasm WASI target, so we need to skip to configure WASI for json-okio and json-tests.
// json-tests uses okio with incorporate with other formatter tests so it is hard and not worth to separate it for two projects for WASI.
// So we disable WASI target in it and we hope, that WASI version of compiler and serialization plugin are identical to the WasmJS target so WASI target is being covered.
val isOkIoOrFormatTests = (name == "kotlinx-serialization-json-okio" || name == "kotlinx-serialization-json-tests")

kotlin {
    jvm {
        withJava()
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += "-Xjdk-release=1.8"
            }
        }
    }

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        compilations.matching { it.name == "main" || it.name == "test" }.configureEach {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
            }
        }
    }

    wasmJs {
        nodejs()
    }

    if (!isOkIoOrFormatTests) {
        wasmWasi {
            nodejs()
        }
    }

    sourceSets.all {
        kotlin.srcDirs("$name/src")
        resources.srcDirs("$name/resources")
        languageSettings {
            progressiveMode = true

            optIn("kotlin.ExperimentalMultiplatform")
            optIn("kotlin.ExperimentalStdlibApi")
            optIn("kotlinx.serialization.InternalSerializationApi")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-common")
                api("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        jvmMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        jvmTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        jsMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-js")
            }
        }

        jsTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
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
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-wasm-js")
            }
        }

        named("wasmJsTest") {
            dependsOn(named("wasmTest").get())
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-wasm-js")
            }
        }

        if (!isOkIoOrFormatTests) {
            named("wasmWasiMain") {
                dependsOn(named("wasmMain").get())
                dependencies {
                    api("org.jetbrains.kotlin:kotlin-stdlib-wasm-wasi")
                }
            }

            named("wasmWasiTest") {
                dependsOn(named("wasmTest").get())
                dependencies {
                    api("org.jetbrains.kotlin:kotlin-test-wasm-wasi")
                }
            }
        }

//        TODO is it needed?
//        nativeMain.dependencies {
//        }
    }

    sourceSets.matching({ it.name.contains("Test") }).configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    sourceSets.matching({ it.name.contains("Main") }).configureEach {
        extra["kotlin.mpp.freeCompilerArgsForSourceSet.${name}"] = listOf("-Xexplicit-api=strict")
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                val kotlin_lv_override = rootProject.extra["kotlin_lv_override"] as String?
                if (kotlin_lv_override != null) {
                    languageVersion = kotlin_lv_override
                    freeCompilerArgs += "-Xsuppress-version-warnings"
                }
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
        compilations["main"].kotlinOptions {
            allWarningsAsErrors = true
        }
    }

    val targetsWithoutTestRunners = listOf("linuxArm64", "linuxArm32Hfp")
    targets.withType(KotlinNativeTargetWithTests::class).configureEach {
        // Configure additional binaries to run tests in the background
        if (listOf("macos", "linux", "mingw").any { name.startsWith(it) && !targetsWithoutTestRunners.contains(name) }) {
            binaries.test("background", listOf(DEBUG)) {
                freeCompilerArgs += listOf("-trw")
                val thisTest = this
                testRuns.create("background") {
                    this as KotlinTaskTestRun<*, *>
                    setExecutionSourceFrom(thisTest)
                    executionTask.configure {
                        this as KotlinNativeTest
                        targetName = "$targetName worker with new MM"
                    }
                }
            }
        }
    }
}

rootProject.extensions.configure<NodeJsRootExtension>() {
    // canary nodejs that supports recent Wasm GC changes
    nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
}