/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class DefaultPolymorphicSerializerTest : JsonTestBase() {

    @Serializable
    abstract class Project {
        abstract val name: String
    }

    @Serializable
    data class DefaultProject(override val name: String, val type: String): Project()

    val module = SerializersModule {
        polymorphic(Project::class) {
            defaultDeserializer { DefaultProject.serializer() }
        }
    }

    private val json = Json { serializersModule = module }

    @Test
    fun test() = parametrizedTest {
        assertEquals(
            DefaultProject("example", "unknown"),
            json.decodeFromString<Project>(""" {"type":"unknown","name":"example"}""", it))
    }

}
