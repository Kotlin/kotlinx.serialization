/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.*
import java.net.URL

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

tasks.withType<DokkaTaskPartial>().named("dokkaHtmlPartial") {
    outputDirectory.set(file("build/dokka"))


    pluginsMapConfiguration.put("org.jetbrains.dokka.base.DokkaBase", """{ "templatesDir": "${rootDir.resolve("dokka-templates").canonicalPath.replace('\\', '/')}" }""")

    dokkaSourceSets {
        configureEach {
            includes.from(rootDir.resolve("dokka/moduledoc.md").path)

            perPackageOption {
                matchingRegex.set("kotlinx\\.serialization(\$|\\.).*")
                reportUndocumented.set(true)
                skipDeprecated.set(true)
            }

            // Internal API
            perPackageOption {
                matchingRegex.set("kotlinx\\.serialization.internal(\$|\\.).*")
                suppress.set(true)
            }

            // Internal JSON API
            perPackageOption {
                matchingRegex.set("kotlinx\\.serialization.json.internal(\$|\\.).*")
                suppress.set(true)
                reportUndocumented.set(false)
            }

            // Workaround for typealias
            perPackageOption {
                matchingRegex.set("kotlinx\\.serialization.protobuf.internal(\$|\\.).*")
                suppress.set(true)
                reportUndocumented.set(false)
            }

            // Deprecated migrations
            perPackageOption {
                matchingRegex.set("kotlinx\\.protobuf(\$|\\.).*")
                reportUndocumented.set(true)
                skipDeprecated.set(true)
            }

            // Deprecated migrations
            perPackageOption {
                matchingRegex.set("org\\.jetbrains\\.kotlinx\\.serialization\\.config(\$|\\.).*")
                reportUndocumented.set(false)
                skipDeprecated.set(true)
            }

            // JS/Native implementation of JVM-only `org.intellij.lang.annotations.Language` class to add syntax support by IDE.
            perPackageOption {
                matchingRegex.set("org\\.intellij\\.lang\\.annotations(\$|\\.).*")
                suppress.set(true)
            }

            sourceLink {
                localDirectory.set(rootDir)

                remoteUrl.set(URL("https://github.com/Kotlin/kotlinx.serialization/tree/master"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}