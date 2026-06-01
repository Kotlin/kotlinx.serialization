/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
rootProject.name = "build-settings-logic"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

apply(from = "src/main/kotlin/serialization-cache-redirector.settings.gradle.kts")
