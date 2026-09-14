/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode

plugins {
    kotlin("multiplatform")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

internal fun Project.versionCatalog(): VersionCatalog = versionCatalogs.named("libs")

kotlin {
    explicitApi()

    compilerOptions {
        progressiveMode = true
        optIn.addAll(
            listOf(
                "kotlin.ExperimentalMultiplatform",
                "kotlin.ExperimentalSubclassOptIn",
                "kotlinx.serialization.InternalSerializationApi",
                "kotlinx.serialization.SealedSerializationApi",
            )
        )
        defaultOptions()
        languageVersion(overriddenLanguageVersion)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm {
        compilerOptions {
            setJava8Compatible()
        }
    }
    jvmToolchain(jdkToolchainVersion)

    js {
        // The part for testing with the latest JS target supported
        val mainCompilation = compilations.getByName("main")
        val testCompilation = compilations.getByName("test")

        val latestJsCompilation = compilations.create("latestJsTest") {
            associateWith(mainCompilation)
            defaultSourceSet.dependsOn(testCompilation.defaultSourceSet)
            binaries.executable(this)
            binaries.configureEach {
                linkTask.configure {
                    compilerOptions {
                        target.set("es2015")
                        moduleKind.set(JsModuleKind.MODULE_UMD) // Mocha adapter doesn't support ES modules yet
                        freeCompilerArgs.add("-Xes-long-as-bigint")
                    }
                }
            }
        }

        nodejs {
            val latestTargetRun = testRuns.create("latestTarget") {
                setExecutionSourceFrom(latestJsCompilation)
                executionTask.configure {
                    val devBinary = latestJsCompilation.binaries
                        .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
                        .single()

                    inputFileProperty.set(devBinary.mainFileSyncPath)
                }
            }

            testTask {
                dependsOn(latestTargetRun.executionTask)
                useMocha {
                    timeout = "10s"
                }
            }
        }

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

    sourceSets {
        commonMain {
            dependencies {
                api(versionCatalog().findLibrary("kotlin.stdlib").get())
            }
        }

        commonTest {
            dependencies {
                implementation(versionCatalog().findLibrary("kotlin.test").get())
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
