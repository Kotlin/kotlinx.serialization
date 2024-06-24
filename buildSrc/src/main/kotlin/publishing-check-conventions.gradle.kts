object TestPublishing {
    const val configurationName = "testRepository"
}

val testRepositoryDependency = configurations.create(TestPublishing.configurationName) {
    isVisible = true
    isCanBeResolved = false
    isCanBeConsumed = false
}


val testRepositories = configurations.create("testRepositories") {
    isVisible = false
    isCanBeResolved = true
    // this config consumes modules from OTHER projects, and cannot be consumed by other projects
    isCanBeConsumed = false

    attributes {
        attribute(Attribute.of("kotlinx.serialization.repository", String::class.java), "test")
    }
    extendsFrom(testRepositoryDependency)
}

tasks.register<ArtifactsCheckTask>("checkArtifacts") {
    repositories.from(testRepositories)
}

abstract class ArtifactsCheckTask: DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repositories: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val artifactsFile = project.rootDir.resolve("gradle/artifacts.txt")

        val actualArtifacts = repositories.files.flatMap { file ->
            file.resolve("org/jetbrains/kotlinx").list()?.toSet() ?: emptySet()
        }.toSortedSet()

        if (project.hasProperty("dumpArtifacts")) {
            artifactsFile.bufferedWriter().use { writer ->
                actualArtifacts.forEach { artifact -> writer.appendLine(artifact) }
            }
            return
        }

        val expectedArtifacts = artifactsFile.readLines().toSet()

        if (expectedArtifacts == actualArtifacts) {
            logger.lifecycle("All artifacts are published")
        } else {
            val missedArtifacts = expectedArtifacts - actualArtifacts
            val unknownArtifacts = actualArtifacts - expectedArtifacts
            val message = "The published artifacts differ from the expected ones." +
                (if (missedArtifacts.isNotEmpty()) missedArtifacts.joinToString(prefix = "\n\tMissing artifacts: ") else "") +
                (if (unknownArtifacts.isNotEmpty()) unknownArtifacts.joinToString(prefix = "\n\tUnknown artifacts: ") else "") +
                "\nTo save current list of artifacts as expecting, call 'checkArtifacts -PdumpArtifacts'"

            logger.error(message)
            throw GradleException("The published artifacts differ from the expected ones")
        }
    }
}
