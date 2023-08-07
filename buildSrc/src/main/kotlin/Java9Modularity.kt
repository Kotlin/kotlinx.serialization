/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin.*
import org.gradle.process.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.*

object Java9Modularity {

    @JvmStatic
    @JvmOverloads
    fun Project.configureJava9ModuleInfo(multiRelease: Boolean = true) {
        val disableJPMS = this.rootProject.extra.has("disableJPMS")
        val ideaActive = System.getProperty("idea.active") == "true"
        if (disableJPMS || ideaActive) return
        val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
        val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }
        if (jvmTargets.isEmpty()) {
            logger.warn("No Kotlin JVM targets found, can't configure compilation of module-info!")
        }
        jvmTargets.forEach { target ->
            val artifactTask = tasks.getByName<Jar>(target.artifactsTaskName) {
                if (multiRelease) {
                    manifest {
                        attributes("Multi-Release" to true)
                    }
                }
            }

            target.compilations.forEach { compilation ->
                val compileKotlinTask = compilation.compileKotlinTask as KotlinCompile
                val defaultSourceSet = compilation.defaultSourceSet

                // derive the names of the source set and compile module task
                val sourceSetName = defaultSourceSet.name + "Module"

                kotlin.sourceSets.create(sourceSetName) {
                    val sourceFile = this.kotlin.find { it.name == "module-info.java" }
                    val targetDirectory = compileKotlinTask.destinationDirectory.map {
                        it.dir("../${it.asFile.name}Module")
                    }

                    // only configure the compilation if necessary
                    if (sourceFile != null) {
                        // register and wire a task to verify module-info.java content
                        //
                        // this will compile the whole sources again with a JPMS-aware target Java version,
                        // so that the Kotlin compiler can do the necessary verifications
                        // while compiling with `jdk-release=1.8` those verifications are not done
                        //
                        // this task is only going to be executed when running with `check` or explicitly,
                        // not during normal build operations
                        val verifyModuleTask = registerVerifyModuleTask(
                            compileKotlinTask,
                            sourceFile
                        )
                        tasks.named("check") {
                            dependsOn(verifyModuleTask)
                        }

                        // register a new compile module task
                        val compileModuleTask = registerCompileModuleTask(
                            compileKotlinTask,
                            sourceFile,
                            targetDirectory
                        )

                        // add the resulting module descriptor to this target's artifact
                        artifactTask.from(compileModuleTask.map { it.destinationDirectory }) {
                            if (multiRelease) {
                                into("META-INF/versions/9/")
                            }
                        }
                    } else {
                        logger.info("No module-info.java file found in ${this.kotlin.srcDirs}, can't configure compilation of module-info!")
                    }

                    // remove the source set to prevent Gradle warnings
                    kotlin.sourceSets.remove(this)
                }
            }
        }
    }

    /**
     * Add a Kotlin compile task that compiles `module-info.java` source file and Kotlin sources together,
     * the Kotlin compiler will parse and check module dependencies,
     * but it currently won't compile to a module-info.class file.
     */
    private fun Project.registerVerifyModuleTask(
        compileTask: KotlinCompile,
        sourceFile: File
    ): TaskProvider<out KotlinJvmCompile> {
        apply<KotlinBaseApiPlugin>()
        val verifyModuleTaskName = "verify${compileTask.name.removePrefix("compile").capitalize()}Module"
        // work-around for https://youtrack.jetbrains.com/issue/KT-60542
        val verifyModuleTask = plugins
            .findPlugin(KotlinBaseApiPlugin::class)!!
            .registerKotlinJvmCompileTask(verifyModuleTaskName)
        verifyModuleTask {
            group = VERIFICATION_GROUP
            description = "Verify Kotlin sources for JPMS problems"
            libraries.from(compileTask.libraries)
            source(compileTask.sources)
            source(compileTask.javaSources)
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            @Suppress("INVISIBLE_MEMBER")
            source(compileTask.scriptSources)
            source(sourceFile)
            destinationDirectory.set(temporaryDir)
            multiPlatformEnabled.set(compileTask.multiPlatformEnabled)
            kotlinOptions {
                moduleName = compileTask.kotlinOptions.moduleName
                jvmTarget = "9"
                freeCompilerArgs += "-Xjdk-release=9"
            }
            // work-around for https://youtrack.jetbrains.com/issue/KT-60583
            inputs.files(
                libraries.asFileTree.elements.map { libs ->
                    libs
                        .filter { it.asFile.exists() }
                        .map {
                            zipTree(it.asFile).filter { it.name == "module-info.class" }
                        }
                }
            ).withPropertyName("moduleInfosOfLibraries")
            this as KotlinCompile
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            @Suppress("DEPRECATION")
            ownModuleName.set(compileTask.kotlinOptions.moduleName)
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            @Suppress("INVISIBLE_MEMBER")
            commonSourceSet.from(compileTask.commonSourceSet)
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            // and work-around for https://youtrack.jetbrains.com/issue/KT-60582
            incremental = false
        }
        return verifyModuleTask
    }

    private fun Project.registerCompileModuleTask(
        compileTask: KotlinCompile,
        sourceFile: File,
        targetDirectory: Provider<out Directory>
    ) = tasks.register("${compileTask.name}Module", JavaCompile::class) {
        // Configure the module compile task.
        source(sourceFile)
        classpath = files()
        destinationDirectory.set(targetDirectory)
        // use a Java 11 toolchain with release 9 option
        // because for some OS / architecture combinations
        // there are no Java 9 builds available
        javaCompiler.set(
            this@registerCompileModuleTask.the<JavaToolchainService>().compilerFor {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        )
        options.release.set(9)

        options.compilerArgumentProviders.add(object : CommandLineArgumentProvider {
            @get:CompileClasspath
            val compileClasspath = compileTask.libraries

            @get:CompileClasspath
            val compiledClasses = compileTask.destinationDirectory

            @get:Input
            val moduleName = sourceFile
                .readLines()
                .single { it.contains("module ") }
                .substringAfter("module ")
                .substringBefore(' ')
                .trim()

            override fun asArguments() = mutableListOf(
                // Provide the module path to the compiler instead of using a classpath.
                // The module path should be the same as the classpath of the compiler.
                "--module-path",
                compileClasspath.asPath,
                "--patch-module",
                "$moduleName=${compiledClasses.get()}",
                "-Xlint:-requires-transitive-automatic"
            )
        })
    }
}
