import java.io.*
import java.util.*

/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
    val trainVersion: String? = providers.gradleProperty("kotlin_snapshot_version").orNull
    val trainVersionFile: String?

    FileInputStream(file("../gradle.properties")).use { propFile ->
        val properties = Properties()
        properties.load(propFile)
        repoVersionFile = properties["kotlin_version"] as String?
        bootstrapVersionFile = properties["kotlin.version.snapshot"] as String?
        trainVersionFile = properties["kotlin_snapshot_version"] as String?
    }

    if (kotlinRepoUrl?.isNotEmpty() == true) {
        return repoVersion ?: repoVersionFile ?: throw IllegalArgumentException("\"kotlin_version\" Gradle property should be defined")
    } else if (bootstrap != null) {
        return bootstrapVersion ?: bootstrapVersionFile ?: throw IllegalArgumentException("\"kotlin.version.snapshot\" Gradle property should be defined")
    }
    if (buildSnapshotTrain?.isNotEmpty() == true) {
        return trainVersion ?: trainVersionFile ?: throw IllegalArgumentException("\"kotlin_snapshot_version\" should be defined when building with snapshot compiler")
    }
    return null
}