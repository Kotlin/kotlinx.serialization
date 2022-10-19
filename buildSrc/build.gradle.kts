/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import java.util.*
import java.io.FileInputStream

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlinVersion = run {
    if (project.hasProperty("build_snapshot_train")) {
        val ver = project.properties["kotlin_snapshot_version"] as? String
        require(!ver.isNullOrBlank()) {"kotlin_snapshot_version must be present if build_snapshot_train is used" }
        return@run ver
    }
    val targetProp = if (project.hasProperty("bootstrap")) "kotlin.version.snapshot" else "kotlin.version"
    FileInputStream(file("../gradle.properties")).use { propFile ->
        val ver = Properties().apply { load(propFile) }[targetProp]
        require(ver is String) { "$targetProp must be string in ../gradle.properties, got $ver instead" }
        ver
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
}

