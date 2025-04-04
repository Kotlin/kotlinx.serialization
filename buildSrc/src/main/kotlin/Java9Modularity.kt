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
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.tooling.core.*
import java.io.*
import kotlin.reflect.*
import kotlin.reflect.full.*

object Java9Modularity {
    private val KotlinProjectExtension.targets: Iterable<KotlinTarget>
        get() = when (this) {
            is KotlinSingleTargetExtension<*> -> listOf(this.target)
            is KotlinMultiplatformExtension -> targets
            else -> error("Unexpected 'kotlin' extension $this")
        }


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
                @Suppress("UNCHECKED_CAST")
                val compileKotlinTask = compilation.compileTaskProvider as TaskProvider<KotlinCompile>
                val defaultSourceSet = compilation.defaultSourceSet

                // derive the names of the source set and compile module task
                val sourceSetName = defaultSourceSet.name + "Module"

                kotlin.sourceSets.create(sourceSetName) {
                    val sourceFile = this.kotlin.find { it.name == "module-info.java" }
                    val targetDirectory = compileKotlinTask.flatMap { task ->
                        task.destinationDirectory.map {
                            it.dir("../${it.asFile.name}Module")
                        }
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
        compileTask: TaskProvider<KotlinCompile>,
        sourceFile: File
    ): TaskProvider<out KotlinJvmCompile> {
        apply<KotlinApiPlugin>()
        @Suppress("DEPRECATION")
        val verifyModuleTaskName = "verify${compileTask.name.removePrefix("compile").capitalize()}Module"
        // work-around for https://youtrack.jetbrains.com/issue/KT-60542
        val kotlinApiPlugin = plugins.getPlugin(KotlinApiPlugin::class)
        val verifyModuleTask = kotlinApiPlugin.registerKotlinJvmCompileTask(
            verifyModuleTaskName,
            compilerOptions = compileTask.get().compilerOptions,
            explicitApiMode = provider { ExplicitApiMode.Disabled }
        )
        verifyModuleTask {
            group = VERIFICATION_GROUP
            description = "Verify Kotlin sources for JPMS problems"
            libraries.from(compileTask.map { it.libraries })
            source(compileTask.map { it.sources })
            source(compileTask.map { it.javaSources })
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            source(compileTask.map {
                @Suppress("INVISIBLE_MEMBER")
                it.scriptSources
            })
            source(sourceFile)
            destinationDirectory.set(temporaryDir)
            multiPlatformEnabled.set(compileTask.get().multiPlatformEnabled)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_9)
                freeCompilerArgs.addAll(
                    listOf("-Xjdk-release=9",  "-Xsuppress-version-warnings", "-Xexpect-actual-classes")
                )
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
            val kotlinPluginVersion = KotlinToolingVersion(kotlinApiPlugin.pluginVersion)
            if (kotlinPluginVersion <= KotlinToolingVersion("1.9.255")) {
                // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
                @Suppress("UNCHECKED_CAST")
                val ownModuleNameProp = (this::class.superclasses.first() as KClass<AbstractKotlinCompile<*>>)
                    .declaredMemberProperties
                    .find { it.name == "ownModuleName" }
                    ?.get(this) as? Property<String>
                ownModuleNameProp?.set(compileTask.flatMap { it.compilerOptions.moduleName})
            }

            val taskKotlinLanguageVersion = compilerOptions.languageVersion.orElse(KotlinVersion.DEFAULT)
            @OptIn(InternalKotlinGradlePluginApi::class)
            if (taskKotlinLanguageVersion.get() < KotlinVersion.KOTLIN_2_0) {
                // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
                @Suppress("INVISIBLE_MEMBER")
                commonSourceSet.from(compileTask.map {
                    @Suppress("INVISIBLE_MEMBER")
                    it.commonSourceSet
                })
            } else {
                multiplatformStructure.refinesEdges.set(compileTask.flatMap { it.multiplatformStructure.refinesEdges })
                multiplatformStructure.fragments.set(compileTask.flatMap { it.multiplatformStructure.fragments })
            }
            // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
            // and work-around for https://youtrack.jetbrains.com/issue/KT-60582
            incremental = false
        }
        return verifyModuleTask
    }

    private fun Project.registerCompileModuleTask(
        compileTask: TaskProvider<KotlinCompile>,
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
            val compileClasspath = objects.fileCollection().from(
                compileTask.map { it.libraries }
            )

            @get:CompileClasspath
            val compiledClasses = compileTask.flatMap { it.destinationDirectory }

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
