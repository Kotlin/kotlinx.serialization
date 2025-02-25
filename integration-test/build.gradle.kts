/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val serialization_version = property("mainLibVersion") as String

// Versions substituted in settings.gradle.kts
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "0"
    id("org.jetbrains.kotlin.plugin.serialization") version "0"
    id("org.jetbrains.kotlin.kapt") version "0"

    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://redirector.kotlinlang.org/maven/dev")
    mavenLocal {
        mavenContent {
            snapshotsOnly()
        }
    }
}

group = "com.example"
version = "0.0.1"

kotlin {
    // Switching module kind for JS is required to run tests
    js {
        nodejs {}
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
    jvm()
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
                // To check that all expected artifacts are resolvable:
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-io:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        jvmMain {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("com.google.dagger:dagger:2.13")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$serialization_version")
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))

            }
        }
        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        named("wasmJsMain") {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-wasm-js")
            }
        }
        named("wasmJsTest") {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-wasm-js")
            }
        }
        named("wasmWasiMain") {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-wasm-wasi")
            }
        }
        named("wasmWasiTest") {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-wasm-wasi")
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                allWarningsAsErrors = true
                // Suppress 'K2 kapt is an experimental feature' warning:
                freeCompilerArgs.add("-Xsuppress-version-warnings")
            }
        }
    }

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

dependencies {
    "kapt"("com.google.dagger:dagger-compiler:2.13")
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
