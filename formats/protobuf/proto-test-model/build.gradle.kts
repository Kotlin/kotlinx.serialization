/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.google.protobuf.gradle.*
import org.gradle.kotlin.dsl.protobuf

plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

protobuf {
    protobuf.protoc {
        // Download from repositories
        artifact = libs.protoc.get().toString()
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(jdkToolchainVersion))

tasks.clean {
    delete(protobuf.protobuf.generatedFilesBaseDir)
}

sourceSets.main {
    extensions.configure<SourceDirectorySet>("proto") {
        srcDirs("testProto", "../jvmTest/resources/common")
    }
}

dependencies {
    api(libs.protobuf.java)
}