/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
    if (project.hasProperty("kotlin_repo_url")) {
        maven(project.properties["kotlin_repo_url"] as String)
    }
    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

dependencies {
    implementation(libs.gradlePlugin.kotlin)

    implementation(libs.gradlePlugin.kover)
    implementation(libs.gradlePlugin.dokka)
}

