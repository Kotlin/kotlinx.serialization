/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import groovy.json.JsonSlurper
import java.io.FileNotFoundException

/**
 * Utility for printing benchmark results.
 * Results can be obtained with JMH flags
 * -rf json -rff serialization-benchmark-results.json
 */
abstract class PrintBenchmarksTask: DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val resultsFile: RegularFileProperty

    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun printBenchmarkJsonAsTeamcityStats() {
        val jsonFile = resultsFile.get().asFile
        if (!jsonFile.exists()) throw TaskExecutionException(this, FileNotFoundException("File $jsonFile not found"))
        val parsedJson = JsonSlurper().parseText(jsonFile.readText()) as Iterable<Map<String, Any>>

        parsedJson.forEach { v ->
            val name = (v["benchmark"] as String).substringAfter("kotlinx.benchmarks.")
            val score = (v["primaryMetric"] as Map<String, Any>)["score"]
            println("##teamcity[buildStatisticValue key='$name' value='$score']")
        }
    }
}

tasks.register<PrintBenchmarksTask>("printBenchmarksJsonAsTeamcityStats") {
    resultsFile.convention(layout.projectDirectory.file("serialization-benchmark-results.json"))
}
