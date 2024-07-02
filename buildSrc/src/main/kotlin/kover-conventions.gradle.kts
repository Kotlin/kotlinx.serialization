/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
    if (hasProperty("kover.enabled") && property("kover.enabled") != "true") {
        disable()
    }

    currentProject {
        projectsForCoverageVerification.forEach { (variantName, _) ->
            // copy the `main` variant for each module to check the coverage only in its section
            copyVariant(variantName, "main")
        }
    }

    merge {
        // collect common coverage for all projects (except excluded) in `main` variant
        subprojects { subproject ->
            subproject.path !in uncoveredProjects
        }
        createVariant("main") { add("jvm", optional = true) }
    }

    reports {
        total.verify.rule("Total coverage") {
            minBound(90)
        }

        projectsForCoverageVerification.forEach { (variantName, projectPath) ->
            variant(variantName) {
                filters.includes.projects.add(projectPath)

                // verify the coverage individually for each module by `check` task
                verify {
                    onCheck = true
                    rule("Coverage for $projectPath") {
                        minBound(85)
                    }
                }
            }
        }
    }
}


val uncoveredProjects get() = setOf(":kotlinx-serialization-bom", ":benchmark", ":guide")
// map: variant name -> project path
val projectsForCoverageVerification get() = mapOf("core" to ":kotlinx-serialization-core", "json" to ":kotlinx-serialization-json", "jsonOkio" to ":kotlinx-serialization-json-okio", "cbor" to ":kotlinx-serialization-cbor", "hocon" to ":kotlinx-serialization-hocon", "properties" to ":kotlinx-serialization-properties", "protobuf" to ":kotlinx-serialization-protobuf", "io" to ":kotlinx-serialization-json-io")
