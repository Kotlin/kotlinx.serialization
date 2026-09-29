/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
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

    @Test
    fun defaultSerializerConflictWithDiscriminatorNotAllowed() = parametrizedTest { mode ->
        @Suppress("UNCHECKED_CAST") val module = SerializersModule {
            polymorphicDefaultSerializer(Project::class) {
                DefaultProject.serializer() as KSerializer<Project>
            }
        }
        val j = Json { serializersModule = module }
        checkEncodingException(mode, {
            j.encodeToString<Project>(DefaultProject("example", "custom"), mode)
        }) {
            message("Class 'kotlinx.serialization.features.DefaultPolymorphicSerializerTest.DefaultProject' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Project>' because it has property name that conflicts with JSON class discriminator 'type'.")
            serialName("kotlinx.serialization.features.DefaultPolymorphicSerializerTest.DefaultProject")
            hint("change class discriminator in JsonConfiguration, or rename property")
        }
    }

}
