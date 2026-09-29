/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import java.net.URI

/**
 * A subset of repositories supported by cache redirector.
 * Taken from https://cache-redirector.jetbrains.com
 */
val cacheMap: Map<String, String> = mapOf(
    "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2",
    "https://repo.maven.apache.org/maven2" to "https://cache-redirector.jetbrains.com/repo1.maven.org/maven2",
    "https://repo1.maven.org/maven2" to "https://cache-redirector.jetbrains.com/repo1.maven.org/maven2",
    "https://clojars.org/repo" to "https://cache-redirector.jetbrains.com/clojars.org/repo",
    "https://dl.google.com/android/repository" to "https://cache-redirector.jetbrains.com/dl.google.com/android/repository",
    "https://dl.google.com/dl/android/maven2" to "https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2",
    "https://github.com/yarnpkg/yarn/releases/download" to "https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download",
    "https://nodejs.org/dist" to "https://cache-redirector.jetbrains.com/nodejs.org/dist",
    "https://registry.npmjs.org" to "https://cache-redirector.jetbrains.com/registry.npmjs.org",
    "https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/dokka/dev",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/eap" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/eap",
    "https://redirector.kotlinlang.org/maven/dev" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev",
    "https://oss.sonatype.org/content/repositories/releases" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/releases",
    "https://oss.sonatype.org/content/repositories/snapshots" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/snapshots",
    "https://oss.sonatype.org/content/repositories/staging" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/staging",
    "https://storage.googleapis.com/r8-releases/raw" to "https://cache-redirector.jetbrains.com/storage.googleapis.com/r8-releases/raw"
)

fun URI.maybeRedirect(): URI {
    if (scheme == "file") return this // skip local repo
    if (host == "cache-redirector.jetbrains.com") return this // already redirected
    val url = toString().trimEnd('/')

    val cacheUrlEntry = cacheMap.entries.find { (origin, _) -> url.startsWith(origin) }
    return if (cacheUrlEntry != null) {
        val cacheUrl = cacheUrlEntry.value
        val originRestPath = url.substringAfter(cacheUrlEntry.key, "")
        URI("$cacheUrl$originRestPath")
    } else {
        logger.info("[Cache redirector] No redirect found for URL: '$this'")
        this
    }
}

fun RepositoryHandler.redirect() = configureEach {
    when (this) {
        is MavenArtifactRepository -> url = url.maybeRedirect()
        is IvyArtifactRepository -> @Suppress("SENSELESS_COMPARISON") /* url is nullable */ if (url != null) {
            url = url.maybeRedirect()
        }
    }
}

pluginManagement.repositories.redirect()
buildscript.repositories.redirect()

gradle.beforeProject {
    buildscript.repositories.redirect()
    repositories.redirect()
}
