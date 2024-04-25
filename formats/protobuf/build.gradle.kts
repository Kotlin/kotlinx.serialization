import Java9Modularity.configureJava9ModuleInfo

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    java // Needed for protobuf plugin only
    kotlin("multiplatform")

    alias(libs.plugins.serialization)
    alias(libs.plugins.protobuf)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

protobuf {
    protoc {
        // Download from repositories
        artifact = libs.protoc.get().toString()
    }
}

tasks.clean {
    delete(protobuf.generatedFilesBaseDir)
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.optIn("kotlinx.serialization.internal.CoreFriendModuleApi")
        }

        commonMain {
            dependencies {
                api(project(":kotlinx-serialization-core"))
            }
        }

        jvmTest {
            kotlin.srcDirs(file("${protobuf.generatedFilesBaseDir}/test/java"))

            dependencies {
                implementation(libs.protobuf.java)
                implementation(libs.kotlintest)
            }
        }
    }
}

sourceSets.test {
    extensions.configure<SourceDirectorySet>("proto") {
        srcDirs("testProto", "jvmTest/resources/common")
    }
}

tasks.named("compileTestKotlinJvm") {
    dependsOn("generateTestProto")
}

configureJava9ModuleInfo()
