/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

/**
 * Serializers produced by the `@Serializer(forClass = ...)` companion shortcut have
 * descriptors without child serializers. Their broken hashCode() used to escape through
 * Json's descriptor-keyed schema cache whenever the alternative-names slow path was
 * taken — the reason JsonCustomSerializersTest historically ran with
 * `useAlternativeNames = false`. With hashCode fixed at the source, the default
 * configuration must work.
 */
class JsonIncompleteDescriptorTest : JsonTestBase() {

    @Serializable(Model.Companion::class)
    data class Model(val a: Int = 0, val b: String = "") {
        @Serializer(forClass = Model::class)
        companion object
    }

    @Test
    fun testUnknownKeysWithDefaultAlternativeNames() = parametrizedTest { mode ->
        // ignoreUnknownKeys + an unknown key forces the deserializationNamesMap
        // slow path (useAlternativeNames is true by default), which caches by
        // descriptor hashCode. Used to throw ArrayIndexOutOfBoundsException.
        val json = Json(default) { ignoreUnknownKeys = true }
        val result = json.decodeFromString<Model>("""{"a":1,"b":"x","unknown":42}""", mode)
        assertEquals(Model(1, "x"), result)
    }
}
