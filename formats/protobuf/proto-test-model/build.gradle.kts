/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

protobuf {
    protoc {
        // Download from repositories
        artifact = libs.protoc.get().toString()
    }
}

// Toolchain version should be the same as JDK release in source-sets-convention
java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

sourceSets.main {
    extensions.configure<SourceDirectorySet>("proto") {
        srcDirs("testProto", "../jvmTest/resources/common")
    }
}

dependencies {
    api(libs.protobuf.java)
}
