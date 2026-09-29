/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@DisableCachingByDefault
abstract class PrepareReleaseTask : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val releaseVersion: Property<String>

    @get:Input
    abstract val previousReleaseVersion: Property<String>

    @TaskAction
    fun prepareRelease() {
        val releaseVersion = releaseVersion.get()
        if (releaseVersion.endsWith("-SNAPSHOT")) {
            throw GradleException("Version isn't specified or incorrect. Please, specify non-snapshot version by adding `-Pversion=1.2.3` argument")
        }

        val rootDir = rootDirectory.get().asFile
        val previousReleaseVersion = previousReleaseVersion.get()
        val snapshotVersion = increaseSnapshotVersion(releaseVersion)

        rootDir.resolve("gradle.properties").patchProperties(releaseVersion, snapshotVersion)
        rootDir.resolve("CHANGELOG.md").patchChangeLog(previousReleaseVersion, releaseVersion)
        rootDir.resolve("README.md").replaceInFile(previousReleaseVersion, releaseVersion)
        rootDir.resolve("integration-test/gradle.properties").patchIntegrationProperties(snapshotVersion)
    }
}

private fun File.patchChangeLog(prevReleaseVersion: String, releaseVersion: String) {
    val oldContent = readText()
    writer().use {
        it.appendLine("$releaseVersion / ${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}")
        it.appendLine("===================")
        it.appendLine()
        it.appendLine("INSERT DESCRIPTION HERE")
        it.appendLine("<<<  Commits  >>>")
        readCommits(prevReleaseVersion).forEach { line ->
            it.appendLine(line)
        }
        it.appendLine("<<<----------->>>")
        it.appendLine()
        it.append(oldContent)
    }
}

private fun File.patchProperties(releaseVersion: String, snapshotVersion: String) {
    val oldLines = readLines()
    writer().use { writer ->
        oldLines.forEach { line ->
            when {
                line.startsWith("version=") -> writer.append("version=").appendLine(snapshotVersion)
                line.startsWith("release.version=") -> writer.append("release.version=").appendLine(releaseVersion)
                else -> writer.appendLine(line)
            }
        }
    }
}

private fun File.patchIntegrationProperties(snapshotVersion: String) {
    val oldLines = readLines()
    writer().use { writer ->
        oldLines.forEach { line ->
            when {
                line.startsWith("mainLibVersion=") -> writer.append("mainLibVersion=").appendLine(snapshotVersion)
                else -> writer.appendLine(line)
            }
        }
    }
}

private fun increaseSnapshotVersion(releaseVersion: String): String {
    val correctedVersion = releaseVersion.substringBefore('-')
    if (correctedVersion != releaseVersion) {
        return "$correctedVersion-SNAPSHOT"
    }

    val parts = correctedVersion.split('.')
    val newVersion = parts.mapIndexed { index, value ->
        if (index == parts.size - 1) (value.toInt() + 1).toString() else value
    }.joinToString(".")

    return "$newVersion-SNAPSHOT"
}

private fun File.replaceInFile(old: String, new: String) {
    val newContent = readText().replace(old, new)
    writeText(newContent)
}

private fun readCommits(prevReleaseVersion: String): List<String> {
    val commitsWithHashes = git("log", "--format=%H%x00%B%x00%x00", "HEAD")
        .split("\u0000\u0000")
        .filter { it.isNotBlank() }
        .associate { record ->
            val (hash, message) = record.split('\u0000', limit = 2)
            hash.trim() to message.trim('\r', '\n')
        }

    val boundaryPrefixes = listOf("prepare $prevReleaseVersion", "release $prevReleaseVersion")
    val boundaryHash = commitsWithHashes.entries.firstOrNull { (_, commitMessage) ->
        val firstLine = commitMessage.lineSequence().first()
        boundaryPrefixes.any { prefix -> firstLine.startsWith(prefix, ignoreCase = true) }
    }?.key

    if (boundaryHash == null) {
        throw GradleException(
            "Unable to find a release for $prevReleaseVersion version. Commit message expected to start with 'prepare $prevReleaseVersion' " +
                "or 'release $prevReleaseVersion' in any charcase"
        )
    }

    val commits = git("log", "--reverse", "--topo-order", "--format=%B%x00", "$boundaryHash..HEAD")
        .split('\u0000')
        .map { it.trim('\r', '\n') }
        .filter { it.isNotEmpty() }

    val issueId = Regex("^(?:(?:fixes|fixed)\\s+)?(#[0-9]+|[0-9]+|KT-[0-9]+)$", RegexOption.IGNORE_CASE)

    return commits.map { commitMessage ->
        val lines = commitMessage.lines()
        val ids = lines.drop(1).mapNotNull { line ->
            issueId.matchEntire(line.trim())?.groupValues?.get(1)
        }
        lines.first() + ids.joinToString(separator = ", ") { id ->
            val url = if (id.startsWith("KT-", ignoreCase = true)) {
                "https://youtrack.jetbrains.com/issue/${id.uppercase()}"
            } else {
                "https://github.com/Kotlin/kotlinx.serialization/issues/${id.removePrefix("#")}"
            }
            "[$id]($url)"
        }
    }
}

private fun git(vararg arguments: String): String {
    val process = ProcessBuilder(*arrayOf("git", *arguments))
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.readBytes().toString(Charsets.UTF_8)

    if (process.waitFor() != 0) {
        throw GradleException("Git error: ${output.trim()}")
    }
    return output
}
