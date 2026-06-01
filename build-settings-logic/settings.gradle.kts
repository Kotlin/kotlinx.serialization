/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
rootProject.name = "build-settings-logic"

dependencyResolutionManagement {
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
    }
}

apply(from = "src/main/kotlin/serialization-cache-redirector.settings.gradle.kts")
