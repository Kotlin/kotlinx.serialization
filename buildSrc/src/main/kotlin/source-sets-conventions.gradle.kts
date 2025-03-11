/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
    explicitApi()

    jvm {
        @Suppress("DEPRECATION") // Migrate on Kotlin 2.1.20 update
        withJava()
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_1_8
                    freeCompilerArgs.add("-Xjdk-release=1.8")
                }
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
            compileTaskProvider.configure {
                compilerOptions {
                    sourceMap = true
                    moduleName = "umd"
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    if (!isOkIoOrFormatTests) {
        @OptIn(ExperimentalWasmDsl::class)
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
    }

    sourceSets.matching({ it.name.contains("Test") }).configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    if (overriddenLanguageVersion != null) {
                        languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
                        freeCompilerArgs.add("-Xsuppress-version-warnings")
                    }
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.allWarningsAsErrors = true
        }
    }
}
