/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*

fun Project.addBomApiDependency(bomProjectPath: String) {
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

