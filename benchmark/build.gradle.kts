import org.gradle.kotlin.dsl.support.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    java
    idea
    kotlin("jvm")
    alias(libs.plugins.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.jmh)
    kotlin("kapt") // For annotation processing
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jmh {
    jmhVersion.set("1.35")
}

tasks.processJmhResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jmhJar {
    archiveBaseName.set("benchmarks")
    archiveVersion.set("")
    archiveClassifier.set("") // benchmarks.jar, not benchmarks-jmh.jar
    destinationDirectory.set(file("$rootDir"))
}

// to include benchmark-module jmh source set compilation during build to verify that it is also compiled succesfully
tasks.assemble {
    dependsOn(tasks.jmhClasses)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        if (overriddenLanguageVersion != null) {
            languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
    }
}

dependencies {
    implementation(libs.jmhCore)
    implementation(libs.guava)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.okio)
    implementation(libs.kotlinx.io)
    implementation(project(":kotlinx-serialization-core"))
    implementation(project(":kotlinx-serialization-cbor"))
    implementation(project(":kotlinx-serialization-json"))
    implementation(project(":kotlinx-serialization-json-okio"))
    implementation(project(":kotlinx-serialization-json-io"))
    implementation(project(":kotlinx-serialization-protobuf"))

    // Moshi
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.codegen)
}
