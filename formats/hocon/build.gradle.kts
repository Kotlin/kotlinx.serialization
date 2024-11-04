import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.dsl.*


/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

kotlin {
    jvmToolchain(jdkToolchainVersion)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        if (overriddenLanguageVersion != null) {
            languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
        freeCompilerArgs.addAll("-Xjdk-release=1.8", "-Xjvm-default=all-compatibility")
    }

    sourceSets.all {
        languageSettings {
            progressiveMode = true

            optIn("kotlinx.serialization.InternalSerializationApi")
        }
    }
}

// Only main
tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":kotlinx-serialization-core"))
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    api(libs.typesafe.config)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.junit4)
}

configureJava9ModuleInfo()
