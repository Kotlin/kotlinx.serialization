import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*

/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

object JavaPluginUtil {

    @JvmStatic
    fun Project.applyJavaPlugin() {
        plugins.apply("java")

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            listOf(
                JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME,
                JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
            ).forEach { outputConfigurationName ->
                configurations.findByName(outputConfigurationName)?.isCanBeConsumed = false
            }

            disableJavaPluginTasks(extensions.getByName("sourceSets") as SourceSetContainer)
        }
    }
}

private fun Project.disableJavaPluginTasks(javaSourceSet: SourceSetContainer) {
    project.tasks.withType(Jar::class.java).named(javaSourceSet.getByName("main").jarTaskName).configure {
        dependsOn("jvmTest")
        enabled = false
    }

    project.tasks.withType(Test::class.java).named(JavaPlugin.TEST_TASK_NAME) {
        dependsOn("jvmJar")
        enabled = false
    }
}
