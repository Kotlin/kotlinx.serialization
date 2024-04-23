import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    java
    kotlin("jvm")
    id("kotlinx-serialization")
    idea
    id("com.github.johnrengelman.shadow")
    id("me.champeau.jmh")
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
    destinationDirectory.set(file("$rootDir"))
}

// to include benchmark-module jmh source set compilation during build to verify that it is also compiled succesfully
tasks.assemble {
    dependsOn(tasks.jmhClasses)
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

dependencies {
    implementation("org.openjdk.jmh:jmh-core:1.35")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("com.squareup.okio:okio:${property("okio_version") as String}")
    implementation(project(":kotlinx-serialization-core"))
    implementation(project(":kotlinx-serialization-json"))
    implementation(project(":kotlinx-serialization-json-okio"))
    implementation(project(":kotlinx-serialization-protobuf"))
}
