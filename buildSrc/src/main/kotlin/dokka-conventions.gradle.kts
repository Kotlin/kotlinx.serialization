/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

val extens = extensions
dependencies {
    dokkaPlugin(provider { extens.getByType<VersionCatalogsExtension>().named("libs").findLibrary("dokka.pathsaver").get().get() })
}

dokka {
    pluginsConfiguration.html {
        templatesDir = rootDir.resolve("dokka-templates")
    }

    dokkaSourceSets.configureEach {
        includes.from(rootDir.resolve("dokka/moduledoc.md").path)
        reportUndocumented = true
        skipDeprecated = true

        perPackageOption {
            matchingRegex = ".*\\.internal(\\..*)?"
            suppress = true
        }

        sourceLink {
            localDirectory = rootDir
            remoteUrl("https://github.com/Kotlin/kotlinx.serialization/tree/master")
        }
    }
}
