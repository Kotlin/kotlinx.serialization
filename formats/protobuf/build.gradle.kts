import Java9Modularity.configureJava9ModuleInfo

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    java // Needed for protobuf plugin only
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("com.google.protobuf")
}

apply(plugin = "native-targets-conventions")
apply(plugin = "source-sets-conventions")


protobuf {
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.17.3"
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
                implementation("com.google.protobuf:protobuf-java:3.17.3")
                implementation("io.kotlintest:kotlintest:2.0.7")
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
