import java.io.*
import java.util.*

/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    repositories {
        /**
         * Overrides for Teamcity 'K2 User Projects' + 'Aggregate build / Kotlinx libraries compilation' configuration:
         * kotlin_repo_url - local repository with snapshot Kotlin compiler
         * kotlin_version - kotlin version to use
         * kotlin_language_version - LV to use
         */
        val kotlinRepoUrl: String? = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl?.isNotEmpty() == true) {
            maven(kotlinRepoUrl)
        }
        /*
        * This property group is used to build kotlinx.serialization against Kotlin compiler snapshot.
        * When build_snapshot_train is set to true, kotlin_version property is used.
        * DO NOT change the name of these properties without adapting kotlinx.train build chain.
        */
        val buildSnapshotTrain: String? = providers.gradleProperty("build_snapshot_train").orNull
        if (buildSnapshotTrain.equals("true", true)) {
            maven("https://oss.sonatype.org/content/repositories/snapshots")
        }

        // kotlin-dev with space redirector
        maven("https://redirector.kotlinlang.org/maven/dev")
        // Cache redirector for gradlePluginPortal
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        // gradlePluginPortal()
        // Cache redirector for mavenCentral
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
        // mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))

            overriddenKotlinVersion()?.also { overriddenVersion ->
                logger.info("Overriding Kotlin version in buildSrc: $overriddenVersion")
                version("kotlin", overriddenVersion)
            }
        }
    }
}

fun overriddenKotlinVersion(): String? {
    val kotlinRepoUrl: String? = providers.gradleProperty("kotlin_repo_url").orNull
    val repoVersion: String? = providers.gradleProperty("kotlin_version").orNull
    val repoVersionFile: String?

    val bootstrap: String? = providers.gradleProperty("bootstrap").orNull
    val bootstrapVersion: String? = providers.gradleProperty("kotlin.version.snapshot").orNull
    val bootstrapVersionFile: String?

    val buildSnapshotTrain: String? = providers.gradleProperty("build_snapshot_train").orNull

    FileInputStream(file("../gradle.properties")).use { propFile ->
        val properties = Properties()
        properties.load(propFile)
        repoVersionFile = properties["kotlin_version"] as String?
        bootstrapVersionFile = properties["kotlin.version.snapshot"] as String?
    }

    if (kotlinRepoUrl?.isNotEmpty() == true) {
        return repoVersion ?: repoVersionFile ?: throw IllegalArgumentException("\"kotlin_version\" Gradle property should be defined")
    } else if (bootstrap != null) {
        return bootstrapVersion ?: bootstrapVersionFile ?: throw IllegalArgumentException("\"kotlin.version.snapshot\" Gradle property should be defined")
    }
    if (buildSnapshotTrain?.isNotEmpty() == true) {
        return repoVersion ?: repoVersionFile ?: throw IllegalArgumentException("\"kotlin_version\" should be defined when building with snapshot compiler")
    }
    return null
}
