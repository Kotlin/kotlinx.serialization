/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*

afterEvaluate {
    val isMultiplatform = plugins.hasPlugin("kotlin-multiplatform")

    if (isMultiplatform) {
        kotlinExtension.sourceSets.getByName("jvmMain").dependencies {
            api(project.dependencies.platform(project(bomProjectPath)))
        }
    } else {
        dependencies {
            "api"(platform(project(bomProjectPath)))
        }
    }
}

val bomProjectPath = ":kotlinx-serialization-bom"
