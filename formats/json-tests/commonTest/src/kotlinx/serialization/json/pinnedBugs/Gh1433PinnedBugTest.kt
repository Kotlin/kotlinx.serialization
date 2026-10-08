/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/1433
 * Pins the current behavior that surfaced in the specific issue.
 *
 * When a sealed/abstract base class has a custom @Serializable(with = ...)
 * JsonContentPolymorphicSerializer, subclass serializers drop the
 * properties of the base class.
 */
class Gh1433PinnedBugTest {
    @Serializable(with = BaseSerializer::class)
    sealed class Base {
        @SerialName("bid")
        var baseId: String? = null
    }

    @Serializable
    class Derived(
        @SerialName("eid")
        var newId: String? = null
    ) : Base()

    object BaseSerializer : JsonContentPolymorphicSerializer<Base>(Base::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Base> {
            return Derived.serializer()
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDecodeDropsBaseClassProperty() {
        val decoded = json.decodeFromString<Base>("""{"bid":"bid","eid":"eid"}""") as Derived
        assertNull(decoded.baseId)
        assertEquals("eid", decoded.newId)
    }

    @Test
    fun testEncodeDropsBaseClassProperty() {
        val toEncode = Derived()
        toEncode.baseId = "bid"
        toEncode.newId = "eid"
        val encoded = json.encodeToString(Base.serializer(), toEncode)
        assertEquals("""{"eid":"eid"}""", encoded)
    }
}
