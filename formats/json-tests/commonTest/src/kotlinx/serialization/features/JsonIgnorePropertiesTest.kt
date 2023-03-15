package kotlinx.serialization.features

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreProperties
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.test.noLegacyJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonIgnorePropertiesTest : JsonTestBase() {

    @Serializable
    @JsonIgnoreProperties("ignored_key")
    data class WithJsonIgnoreProperties(val data: String)

    @Serializable
    @JsonIgnoreProperties("ignored_key")
    abstract class BaseMessage

    @Serializable
    data class ConcreteMessage(val message: String) : BaseMessage()

    @Test
    fun testIgnoredPropertiesAreIgnored() = noLegacyJs {
        val serialized = """{"data":"test","ignored_key":"ignored"}"""
        val expected = WithJsonIgnoreProperties(data = "test")

        assertDeserializingJson(
            WithJsonIgnoreProperties.serializer(),
            expected,
            serialized
        )
    }

    @Test
    fun testMissingIgnoredPropertiesAreIgnored() = noLegacyJs {
        val serialized = """{"data":"test"}"""
        val expected = WithJsonIgnoreProperties(data = "test")

        assertDeserializingJson(
            WithJsonIgnoreProperties.serializer(),
            expected,
            serialized
        )
    }

    @Test
    fun testUnknownPropertyThrowsSerializationException() = noLegacyJs {
        val serialized = """{"data":"test","key":"value"}"""

        parametrizedTest { jsonTestingMode ->
            assertFailsWith<SerializationException>(
                message = "Failed with streaming = $jsonTestingMode"
            ) {
                default.decodeFromString(
                    deserializer = WithJsonIgnoreProperties.serializer(),
                    source = serialized,
                    jsonTestingMode = jsonTestingMode
                )
            }
        }
    }

    @Test
    fun testInheritedIgnoredPropertiesAreIgnored() = noLegacyJs {
        val serialized = """{"message":"test","ignored_key":"ignored"}"""
        val expected = ConcreteMessage(message = "test")

        assertDeserializingJson(
            ConcreteMessage.serializer(),
            expected,
            serialized
        )
    }

    /**
     * Tests both json converters (streaming and tree) using [parametrizedTest]
     */
    private fun <T> assertDeserializingJson(
        serializer: KSerializer<T>,
        data: T,
        serialized: String,
        json: Json = default,
    ) {
        parametrizedTest { jsonTestingMode ->
            val deserialized: T = json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertEquals(data, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
}