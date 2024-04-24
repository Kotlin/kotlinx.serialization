/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kotlinx-serialization"

include(":kotlinx-serialization-core")
project(":kotlinx-serialization-core").projectDir = file("./core")

include(":kotlinx-serialization-bom")
project(":kotlinx-serialization-bom").projectDir = file("./bom")

include(":kotlinx-serialization-json")
project(":kotlinx-serialization-json").projectDir = file("./formats/json")

include(":kotlinx-serialization-json-okio")
project(":kotlinx-serialization-json-okio").projectDir = file("./formats/json-okio")

include(":kotlinx-serialization-json-tests")
project(":kotlinx-serialization-json-tests").projectDir = file("./formats/json-tests")

include(":kotlinx-serialization-protobuf")
project(":kotlinx-serialization-protobuf").projectDir = file("./formats/protobuf")

include(":kotlinx-serialization-cbor")
project(":kotlinx-serialization-cbor").projectDir = file("./formats/cbor")

include(":kotlinx-serialization-hocon")
project(":kotlinx-serialization-hocon").projectDir = file("./formats/hocon")

include(":kotlinx-serialization-properties")
project(":kotlinx-serialization-properties").projectDir = file("./formats/properties")

include(":benchmark")
project(":benchmark").projectDir = file("./benchmark")

include(":guide")
project(":guide").projectDir = file("./guide")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            overriddenKotlinVersion()?.also { overriddenVersion ->
                version("kotlin", overriddenVersion)
            }
        }
    }
}

fun overriddenKotlinVersion(): String? {
    val kotlinRepoUrl: String? = providers.gradleProperty("kotlin_repo_url").orNull
    val repoVersion: String? = providers.gradleProperty("kotlin_version").orNull

    val bootstrap: String? = providers.gradleProperty("bootstrap").orNull
    val bootstrapVersion: String? = providers.gradleProperty("kotlin.version.snapshot").orNull

    val buildSnapshotTrain: String? = providers.gradleProperty("build_snapshot_train").orNull
    val trainVersion: String? = providers.gradleProperty("kotlin_snapshot_version").orNull

    if (kotlinRepoUrl?.isNotEmpty() == true) {
        return repoVersion ?: throw IllegalArgumentException("\"kotlin_version\" Gradle property should be defined")
    } else if (bootstrap != null) {
        return bootstrapVersion ?: throw IllegalArgumentException("\"kotlin.version.snapshot\" Gradle property should be defined")
    }
    if (buildSnapshotTrain?.isNotEmpty() == true) {
        return trainVersion ?: throw IllegalArgumentException("\"kotlin_snapshot_version\" should be defined when building with snapshot compiler")
    }
    return null
}