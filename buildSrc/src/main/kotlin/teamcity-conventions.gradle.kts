/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*

val teamcitySuffix = findProperty("teamcitySuffix")?.toString()
if (!(rootProject.extra["teamcityInteractionDisabled"] as Boolean) && hasProperty("teamcity") && !((rootProject.extra["build_snapshot_train"] as Boolean) || hasProperty("build_snapshot_up"))) {
    // Tell teamcity about version number
    val postfix = if (teamcitySuffix == null) "" else " ($teamcitySuffix)"
    println("##teamcity[buildNumber '${project.version}${postfix}']")

    gradle.taskGraph.beforeTask {
        println("##teamcity[progressMessage 'Gradle: ${path}:${name}']")
    }
}
