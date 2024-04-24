/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.*

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
