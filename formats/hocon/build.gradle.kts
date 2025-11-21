import Java9Modularity.configureJava9ModuleInfo


/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

kotlin {
    explicitApi()
    jvmToolchain(jdkToolchainVersion)

    compilerOptions {
        defaultOptions()
        setJava8Compatible()
        languageVersion(overriddenLanguageVersion)

        progressiveMode = true
        optIn.add("kotlinx.serialization.InternalSerializationApi")
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
