/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*

val teamcitySuffix = findProperty("teamcitySuffix")?.toString()
if (teamcityInteractionEnabled && hasProperty("teamcity") && !propertyIsTrue("build_snapshot_train")) {
    // Tell teamcity about version number
    val postfix = if (teamcitySuffix == null) "" else " ($teamcitySuffix)"
    println("##teamcity[buildNumber '${project.version}${postfix}']")

    gradle.taskGraph.beforeTask {
        println("##teamcity[progressMessage 'Gradle: ${path}:${name}']")
    }
}
