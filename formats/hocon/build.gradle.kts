import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import Java9Modularity.configureJava9ModuleInfo


/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
}

tasks.compileKotlin {
    kotlinOptions {
        allWarningsAsErrors = true
        jvmTarget = "1.8"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        val kotlin_lv_override = rootProject.extra["kotlin_lv_override"] as String?
        if (kotlin_lv_override != null) {
            languageVersion = kotlin_lv_override
            freeCompilerArgs += "-Xsuppress-version-warnings"
        }
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

    api("com.typesafe:config:1.4.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("junit:junit:4.12")
}

configureJava9ModuleInfo()
