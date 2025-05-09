/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    /**
     * Overrides for Teamcity 'K2 User Projects' + 'Aggregate build / Kotlinx libraries compilation' configuration:
     * kotlin_repo_url - local repository with snapshot Kotlin compiler
     * kotlin_version - kotlin version to use
     * kotlin_language_version - LV to use
     */
    val snapshotRepoUrl = findProperty("kotlin_repo_url") as String?
    if (snapshotRepoUrl?.isNotEmpty() == true) {
        maven(snapshotRepoUrl)
    }
    /*
    * This property group is used to build kotlinx.serialization against Kotlin compiler snapshot.
    * When build_snapshot_train is set to true, kotlin_version property is overridden with kotlin_snapshot_version.
    * DO NOT change the name of these properties without adapting kotlinx.train build chain.
    */
    if ((findProperty("build_snapshot_train") as? String?).equals("true", true)) {
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://redirector.kotlinlang.org/maven/dev")

    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    // For Dokka that depends on kotlinx-html
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")

    mavenCentral()
    mavenLocal()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.kover)
    implementation(libs.gradlePlugin.dokka)
    implementation(libs.gradlePlugin.animalsniffer)
    implementation(libs.gradlePlugin.binaryCompatibilityValidator)
}
