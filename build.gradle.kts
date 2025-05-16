/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import kotlinx.validation.*
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    base
    alias(libs.plugins.knit)
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("org.jetbrains.dokka")
    id("benchmark-conventions")
    id("publishing-check-conventions")
    id("kover-conventions")

    alias(libs.plugins.serialization) apply false
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://redirector.kotlinlang.org/maven/dev")
    // For Dokka that depends on kotlinx-html
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    // For local development
    mavenLocal()
}

// == common projects settings setup
allprojects {
    // group setup
    group = "org.jetbrains.kotlinx"

    // version setup
    val deployVersion = properties["DeployVersion"]
    if (deployVersion != null) version = deployVersion
    if (project.hasProperty("bootstrap")) {
        version = "$version-SNAPSHOT"
    }

    // repositories setup
    if (propertyIsTrue("build_snapshot_train")) {
        // Snapshot-specific
        repositories {
            mavenLocal()
            maven("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
    val snapshotRepoUrl = findProperty("kotlin_repo_url")
    if (snapshotRepoUrl != null && snapshotRepoUrl != "") {
        // Snapshot-specific for K2 CI configurations
        repositories {
            maven(snapshotRepoUrl)
        }
    }
    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://redirector.kotlinlang.org/maven/dev")
    }
}

// == BCV setup ==
apiValidation {
    ignoredProjects.addAll(listOf("benchmark", "guide", "kotlinx-serialization", "kotlinx-serialization-json-tests"))
    @OptIn(ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

// == Knit setup ==

knit {
    siteRoot = "https://kotlinlang.org/api/kotlinx.serialization"
    moduleDocs = "build/dokka-module/html/module"
    dokkaMultiModuleRoot = "build/dokka/html/"
}

// Build API docs for all modules with dokka before running Knit
tasks.named("knitPrepare") {
    dependsOn("dokkaGenerate")
}


// == compiler flags setup ==

subprojects {
    apply(plugin = "global-compiler-options")
}

// == TeamCity setup ==
subprojects {
    apply(plugin = "teamcity-conventions")
}

// == publishing setup ==
subprojects {
    if (name in unpublishedProjects) return@subprojects
    apply(plugin = "publishing-conventions")
}

// == publishing setup ==

val mergeProject = project

subprojects {
    if (name in unpublishedProjects) return@subprojects
    apply(plugin = "publishing-conventions")
    mergeProject.dependencies.add(Publishing_check_conventions_gradle.TestPublishing.configurationName, this)
}

// == animalsniffer setup ==
subprojects {
    // Can't be applied to BOM
    if (project.name in excludedFromBomProjects) return@subprojects
    apply(plugin = "animalsniffer-conventions")
}

// == BOM setup ==
subprojects {
    // Can't be applied to BOM
    if (project.name in excludedFromBomProjects) return@subprojects
    apply(plugin = "bom-conventions")
}

// == Dokka setup ==
subprojects {
    if (name in documentedSubprojects) {
        apply(plugin = "dokka-conventions")
    }
}

// apply conventions to root module and setup dependencies for aggregation
apply(plugin = "dokka-conventions")
dependencies {
    subprojects.forEach {
        if (it.name in documentedSubprojects) {
            dokka(it)
        }
    }
}

// == NPM setup ==

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

// == KGP version setup ==
logger.warn("Project is using Kotlin Gradle plugin version: ${project.getKotlinPluginVersion()}")

// == projects lists and flags ==
// getters are required because of variable lazy initialization in Gradle
val unpublishedProjects
    get() = setOf(
        "benchmark",
        "guide",
        "kotlinx-serialization-json-tests",
        "proto-test-model",
    )
val excludedFromBomProjects get() = unpublishedProjects + "kotlinx-serialization-bom"

val documentedSubprojects
    get() = setOf(
        "kotlinx-serialization-core",
        "kotlinx-serialization-json",
        "kotlinx-serialization-json-okio",
        "kotlinx-serialization-json-io",
        "kotlinx-serialization-cbor",
        "kotlinx-serialization-properties",
        "kotlinx-serialization-hocon",
        "kotlinx-serialization-protobuf"
    )
