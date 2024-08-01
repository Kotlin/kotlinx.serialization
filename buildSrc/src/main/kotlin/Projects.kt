/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.*
import org.gradle.api.tasks.*

val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

fun Project.propertyIsTrue(propertyName: String): Boolean {
    return (findProperty(propertyName) as? String?).equals("true", true)
}

val Project.jdkToolchainVersion: Int get() = findProperty("jdk_toolchain_version").toString().toInt()

val Project.overriddenLanguageVersion : String?
    get() = findProperty("kotlin_language_version") as String?

val Project.teamcityInteractionEnabled : Boolean
    get() = !hasProperty("no_teamcity_interaction") && !hasProperty("build_snapshot_up")
