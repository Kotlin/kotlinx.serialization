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

    reports {
        verify {
            rule("Minimal line coverage rate in percents") {

                // Core is mainly uncovered because a lot of serializers are tested with JSON
                val minPercentage = if (project.name.contains("core") || project.name.contains("properties") || project.name.contains("json-okio")) 44 else 80
                minBound(minPercentage)
                // valueType is 'COVERED_LINES_PERCENTAGE' by default
            }
        }
    }
}
