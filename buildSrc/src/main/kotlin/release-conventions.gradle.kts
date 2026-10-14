/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

tasks.register<PrepareReleaseTask>("prepareRelease") {
    rootDirectory.set(layout.projectDirectory)
    releaseVersion.set(project.version.toString())
    previousReleaseVersion.set(providers.gradleProperty("release.version"))
}
