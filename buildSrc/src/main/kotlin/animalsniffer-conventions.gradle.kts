import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.testing.*
import org.gradle.kotlin.dsl.*
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// Animalsniffer setup
plugins {
    id("ru.vyarus.animalsniffer")
}


plugins.withId("org.jetbrains.kotlin.multiplatform") {
    listOf(
        JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME,
        JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
    ).forEach { outputConfigurationName ->
        configurations.findByName(outputConfigurationName)?.isCanBeConsumed = false
    }
}


afterEvaluate { // Can be applied only when the project is evaluated
    extensions.configure<AnimalSnifferExtension> {
        sourceSets = listOfNotNull(
            this@afterEvaluate.sourceSets.findByName("jvmMain"),
            this@afterEvaluate.sourceSets.findByName("main")
        )

        val annotationValue = when(name) {
            "kotlinx-serialization-core" -> "kotlinx.serialization.internal.SuppressAnimalSniffer"
            "kotlinx-serialization-hocon" -> "kotlinx.serialization.hocon.internal.SuppressAnimalSniffer"
            "kotlinx-serialization-protobuf" -> "kotlinx.serialization.protobuf.internal.SuppressAnimalSniffer"
            "kotlinx-serialization-cbor" -> "kotlinx.serialization.cbor.internal.SuppressAnimalSniffer"
            else -> "kotlinx.serialization.json.internal.SuppressAnimalSniffer"
        }

        annotation = annotationValue
    }
    dependencies {
        "signature"("net.sf.androidscents.signature:android-api-level-14:4.0_r4@signature")
        "signature"("org.codehaus.mojo.signature:java18:1.0@signature")
    }

}
