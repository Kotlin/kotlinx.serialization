import com.android.tools.r8.*
import com.android.tools.r8.origin.*
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.inject.Inject

/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
buildscript {
    repositories {
        mavenCentral()
        // Using Google Cloud Storage, see: https://r8.googlesource.com/r8#obtaining-prebuilts
        maven("https://storage.googleapis.com/r8-releases/raw")
    }

    dependencies {
        // `8.10` corresponds to Kotlin `2.2`, see: https://developer.android.com/build/kotlin-support
        classpath("com.android.tools:r8:8.10.21")
    }
}

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

kotlin {
    // use toolchain from settings
    jvmToolchain(jdkToolchainVersion)
    compilerOptions {
        defaultOptions()
        // Do not set jvmTarget=1.8 here, as it conflicts with jvmToolchain
        languageVersion(overriddenLanguageVersion)
    }
}

val sharedSourceSet = sourceSets.create("shared") {
    kotlin.srcDirs("src/shared")
}

val r8FullModeSourceSet = sourceSets.create("testR8FullMode") {
    kotlin.srcDirs(sharedSourceSet.kotlin.srcDirs)
}

val proguardCompatibleSourceSet = sourceSets.create("testProguardCompatible") {
    kotlin.srcDirs(sharedSourceSet.kotlin.srcDirs)
}

val sharedImplementation = configurations.getByName("sharedImplementation")

dependencies {
    sharedImplementation(project(":kotlinx-serialization-core"))
    sharedImplementation("org.jetbrains.kotlin:kotlin-test")
    sharedImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    sharedImplementation(libs.junit.junit4)
    sharedImplementation(kotlin("test-junit"))
}

// extend sharedImplementation by all test compilation tasks
configurations.getByName("testR8FullModeImplementation") {
    extendsFrom(sharedImplementation)
}
configurations.getByName("testProguardCompatibleImplementation") {
    extendsFrom(sharedImplementation)
}

tasks.withType<KotlinCompile>().named("compileTestR8FullModeKotlin") {
    configureCompilation(r8FullMode = true)
}

tasks.withType<KotlinCompile>().named("compileTestProguardCompatibleKotlin") {
    configureCompilation(r8FullMode = false)
}

val testR8FullMode = tasks.register("testR8FullMode", Test::class) {
    group = "verification"
    testClassesDirs = r8FullModeSourceSet.output.classesDirs
    classpath = r8FullModeSourceSet.runtimeClasspath
    configureTest(r8FullMode = true)
}

val testProguardCompatible = tasks.register("testProguardCompatible", Test::class) {
    group = "verification"
    testClassesDirs = proguardCompatibleSourceSet.output.classesDirs
    classpath = proguardCompatibleSourceSet.runtimeClasspath
    configureTest(r8FullMode = false)
}

tasks.check {
    dependsOn(testR8FullMode)
    dependsOn(testProguardCompatible)
}

//
// R8 actions
//
/**
 * Get jar with standard Java classes.
 * For JDK > 9 these classes are located in the `base` module.
 * The module has the special format `jmod` and it isn't supported in R8, so we should convert content of jmod to jar.
 */
abstract class ExtractBaseJar : DefaultTask() {
    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun extract() {
        val javaHomeDir = javaLauncher.get().metadata.installationPath.asFile
        val baseJmod = javaHomeDir.resolve("jmods").resolve("java.base.jmod")
        if (!baseJmod.exists()) {
            throw GradleException("Cannot find file with base java module, make sure that specified jdk_toolchain_version is 9 or higher")
        }

        val extractDir = temporaryDir.resolve("java-base")

        extractDir.deleteRecursively()
        extractDir.mkdirs()
        // unpack jmod file
        val jdkBinDir = javaHomeDir.resolve("bin")

        val jmodFile = if (System.getProperty("os.name").startsWith("Windows")) {
            jdkBinDir.resolve("jmod.exe")
        } else {
            jdkBinDir.resolve("jmod")
        }

        execOperations.exec {
            commandLine(jmodFile.absolutePath, "extract", baseJmod.absolutePath, "--dir", extractDir.absolutePath)
        }
        // pack class-files into jar
        execOperations.exec {
            commandLine(
                "jar",
                "--create",
                "--file",
                outputJar.get().asFile.absolutePath,
                "-C",
                File(extractDir, "classes").absolutePath,
                "."
            )
        }
    }
}

val baseJar = layout.buildDirectory.file("jdk/java.base.jar")
val extractBaseJarTask = tasks.register<ExtractBaseJar>("extractBaseJar") {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(jdkToolchainVersion))
        }
    )
    outputJar.set(baseJar)
}

// Serialization ProGuard/R8 rules
val ruleFiles = setOf(projectDir.resolve("../common.pro"), projectDir.resolve("../r8.pro"))

/**
 * Configure replacing original class-files with classes processed by R8
 */
fun KotlinCompile.configureCompilation(r8FullMode: Boolean) {
    // R8 output files
    val mode = if (r8FullMode) "full" else "compatible"
    val mapFile = layout.buildDirectory.file("r8/$mode/mapping.txt")
    val usageFile = layout.buildDirectory.file("r8/$mode/usage.txt")
    val runtimeDependencies = project.files(
        project.configurations.getByName("runtimeClasspath"),
        project.configurations.getByName("sharedRuntimeClasspath")
    )

    dependsOn(extractBaseJarTask)
    inputs.files(baseJar)
    inputs.files(ruleFiles)

    outputs.file(mapFile)
    outputs.file(usageFile)

    // disable incremental compilation because previously compiled classes may be deleted or renamed by R8
    incremental = false

    doLast(
        R8Action(
            runtimeDependencies,
            baseJar.get().asFile,
            ruleFiles,
            mapFile.get().asFile,
            usageFile.get().asFile,
            r8FullMode
        )
    )
}

fun Test.configureTest(r8FullMode: Boolean) {
    // R8 output files
    val mode = if (r8FullMode) "full" else "compatible"
    val mapFile = project.layout.buildDirectory.file("r8/$mode/mapping.txt")
    val usageFile = project.layout.buildDirectory.file("r8/$mode/usage.txt")

    doFirst {
        systemProperty("r8.output.map", mapFile.get().asFile.absolutePath)
        systemProperty("r8.output.usage", usageFile.get().asFile.absolutePath)
    }
}

class R8Action(
    private val runtimeDependencies: FileCollection,
    private val baseJar: File,
    private val ruleFiles: Set<File>,
    private val mapFile: File,
    private val usageFile: File,
    private val fullMode: Boolean
) : Action<Task> {
    override fun execute(task: Task) {
        val compileTask = task as KotlinCompile
        val intermediateDir = task.temporaryDir.resolve("original")
        val kotlinOutput = compileTask.destinationDirectory.get().asFile

        intermediateDir.deleteRecursively()
        // copy original class-files to temp dir
        kotlinOutput.walk()
            .filter { file -> file.isFile && file.extension == "class" }
            .forEach { file ->
                val relative = file.toRelativeString(kotlinOutput)
                val targetFile = intermediateDir.resolve(relative)

                targetFile.parentFile.mkdirs()
                file.copyTo(targetFile)
                file.delete()
            }

        val classFiles = intermediateDir.walk().filter { it.isFile }.toList()

        run(
            kotlinOutput,
            classFiles,
            runtimeDependencies.files + baseJar,
            ruleFiles,
            mapFile,
            usageFile,
            fullMode
        )
    }

    private fun run(
        outputDir: File,
        originalClasses: List<File>,
        libraries: Set<File>,
        ruleFiles: Set<File>,
        mapFile: File,
        usageFile: File,
        fullMode: Boolean = true
    ) {
        val r8Command = R8Command.builder(DiagnosticLogger())
            .addProgramFiles(originalClasses.map { it.toPath() })
            .addLibraryFiles(libraries.map { it.toPath() })
            .addProguardConfigurationFiles(ruleFiles.map { file -> file.toPath() })
            .addProguardConfiguration(
                listOf(
                    "-keep class **.*Tests { *; }",
                    // widespread rule in AGP
                    "-allowaccessmodification",
                    // on some OS mixed classnames may lead to problems due classes like a/a and a/A cannot be stored simultaneously in their file system
                    "-dontusemixedcaseclassnames",
                    // uncomment to show reason of keeping specified class
                    //"-whyareyoukeeping class YourClassName",
                ),
                object : Origin(root()) {
                    override fun part() = "EntryPoint"
                })

            .setDisableTreeShaking(false)
            .setDisableMinification(false)
            .setProguardCompatibility(!fullMode)

            .setProgramConsumer(ClassFileConsumer.DirectoryConsumer(outputDir.toPath()))

            .setProguardMapConsumer(StringConsumer.FileConsumer(mapFile.toPath()))
            .setProguardUsageConsumer(StringConsumer.FileConsumer(usageFile.toPath()))
            .build()

        R8.run(r8Command)
    }
}

class DiagnosticLogger : DiagnosticsHandler {
    private val logger = Logging.getLogger(DiagnosticLogger::class.java)

    override fun warning(diagnostic: Diagnostic) {
        // we shouldn't ignore any warning in R8
        throw GradleException("Warning in R8: ${diagnostic.format()}")
    }

    override fun error(diagnostic: Diagnostic) {
        throw GradleException("Error in R8: ${diagnostic.format()}")
    }

    override fun info(diagnostic: Diagnostic) {
        logger.info("Info in R8: ${diagnostic.format()}")
    }

    private fun Diagnostic.format(): String {
        return "$diagnosticMessage\nIn: $position\nFrom: ${this.origin}"
    }
}
