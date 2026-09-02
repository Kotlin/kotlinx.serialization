plugins {
    `java-platform`
}

val name = project.name

dependencies {
    constraints {
        rootProject.subprojects.forEach {
            if (it.name == name) return@forEach
            if (!it.plugins.hasPlugin("maven-publish")) return@forEach
            evaluationDependsOn(it.path)
            it.publishing.publications.all {
                this as MavenPublication
                if (artifactId.endsWith("-kotlinMultiplatform")) return@all
                if (artifactId.endsWith("-metadata")) return@all
                // Skip platform artifacts (like *-linuxx64, *-macosx64)
                // It leads to inconsistent bom when publishing from different platforms
                // (e.g. on linux it will include only linuxx64 artifacts and no macosx64)
                // It shouldn't be a problem as usually consumers need to use generic *-native artifact
                // Gradle will choose correct variant by using metadata attributes
                if (artifacts.any { it.extension == "klib" }) return@all
                this@constraints.api(mapOf("group" to groupId, "name" to artifactId, "version" to version))
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])
        }
    }
}

// Disable metadata publication
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}
