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
    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

val kotlinVersion = FileInputStream(file("../gradle.properties")).use { propFile ->
    val ver = project.findProperty("kotlin.version")?.toString()
        ?: Properties().apply { load(propFile) }["kotlin.version"]
    require(ver is String) { "kotlin.version must be string in ../gradle.properties, got $ver instead" }
    ver
}

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
}

