import org.gradle.api.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import java.io.*

object Java9Modularity {

    @JvmStatic
    @JvmOverloads
    fun Project.configureJava9ModuleInfo(multiRelease: Boolean = true) {
        val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
        val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*> }
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
                val compileKotlinTask = compilation.compileKotlinTask as AbstractCompile
                val defaultSourceSet = compilation.defaultSourceSet

                // derive the names of the source set and compile module task
                val sourceSetName = defaultSourceSet.name + "Module"
                val compileModuleTaskName = compileKotlinTask.name + "Module"

                kotlin.sourceSets.create(sourceSetName) {
                    val sourceFile = this.kotlin.find { it.name == "module-info.java" }
                    val targetFile = compileKotlinTask.destinationDirectory.file("../module-info.class").get().asFile

                    // only configure the compilation if necessary
                    if (sourceFile != null) {
                        // the default source set depends on this new source set
                        defaultSourceSet.dependsOn(this)

                        // register a new compile module task
                        val compileModuleTask = registerCompileModuleTask(compileModuleTaskName, compileKotlinTask, sourceFile, targetFile)

                        // add the resulting module descriptor to this target's artifact
                        artifactTask.dependsOn(compileModuleTask)
                        artifactTask.from(targetFile) {
                            if (multiRelease) {
                                into("META-INF/versions/9/")
                            }
                        }
                    } else {
                        logger.info("No module-info.java file found in ${this.kotlin.srcDirs}, can't configure compilation of module-info!")
                        // remove the source set to prevent Gradle warnings
                        kotlin.sourceSets.remove(this)
                    }
                }
            }
        }
    }

    private fun Project.registerCompileModuleTask(taskName: String, compileTask: AbstractCompile, sourceFile: File, targetFile: File) =
        tasks.register(taskName, JavaCompile::class) {
            // Also add the module-info.java source file to the Kotlin compile task;
            // the Kotlin compiler will parse and check module dependencies,
            // but it currently won't compile to a module-info.class file.
            compileTask.source(sourceFile)


            // Configure the module compile task.
            dependsOn(compileTask)
            source(sourceFile)
            outputs.file(targetFile)
            classpath = files()
            destinationDirectory.set(compileTask.destinationDirectory)
            sourceCompatibility = JavaVersion.VERSION_1_9.toString()
            targetCompatibility = JavaVersion.VERSION_1_9.toString()

            doFirst {
                // Provide the module path to the compiler instead of using a classpath.
                // The module path should be the same as the classpath of the compiler.
                options.compilerArgs = listOf(
                    "--release", "9",
                    "--module-path", compileTask.classpath.asPath,
                    "-Xlint:-requires-transitive-automatic"
                )
            }

            doLast {
                // Move the compiled file out of the Kotlin compile task's destination dir,
                // so it won't disturb Gradle's caching mechanisms.
                val compiledFile = destinationDirectory.file(targetFile.name).get().asFile
                targetFile.parentFile.mkdirs()
                compiledFile.renameTo(targetFile)
            }
        }
}
