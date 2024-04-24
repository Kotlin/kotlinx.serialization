import org.gradle.kotlin.dsl.*

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

gradle.taskGraph.whenReady {
    println("Using Kotlin compiler version: ${extra["compilerVersion"]}")
    if (rootProject.extra["build_snapshot_train"] as Boolean) {
        subprojects {
            if (name != "core") return@subprojects

            configurations.matching { it.name == "kotlinCompilerClasspath" }.configureEach {
                println("Manifest of kotlin-compiler-embeddable.jar for serialization")
                resolvedConfiguration.files.filter { it.name.contains("kotlin-compiler-embeddable") }.forEach { file ->
                    val manifest = zipTree(file).matching {
                        include("META-INF/MANIFEST.MF")
                    }.files.first()

                    manifest.readLines().forEach { line ->
                        println(line)
                    }
                }
            }
        }
    }
}
