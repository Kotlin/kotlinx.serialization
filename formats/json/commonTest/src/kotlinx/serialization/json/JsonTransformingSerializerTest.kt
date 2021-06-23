/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class JsonTransformingSerializerTest : JsonTestBase() {
    val json = Json { encodeDefaults = false }

    @Serializable
    data class Example(
        val name: String,
        @Serializable(UnwrappingJsonListSerializer::class) val data: StringData,
        @SerialName("more_data") @Serializable(WrappingJsonListSerializer::class) val moreData: List<StringData> = emptyList()
    )

    object WrappingJsonListSerializer :
        JsonTransformingSerializer<List<StringData>>(ListSerializer(StringData.serializer())) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            if (element !is JsonArray) JsonArray(listOf(element)) else element
    }

    object UnwrappingJsonListSerializer :
        JsonTransformingSerializer<StringData>(StringData.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            if (element !is JsonArray) return element
            require(element.size == 1) { "Array size must be equal to 1 to unwrap it" }
            return element.first()
        }
    }

    object DroppingNameSerializer : JsonTransformingSerializer<Example>(Example.serializer()) {
        override fun transformSerialize(element: JsonElement): JsonElement =
            JsonObject(element.jsonObject.filterNot { (k, v) -> k == "name" && v.jsonPrimitive.content == "First" })
    }

    @Test
    fun testExampleCanBeParsed() = parametrizedTest { streaming ->
        val testDataInput = listOf(
            """{"name":"test","data":{"data":"str1"},"more_data":[{"data":"str2"}]}""",
            """{"name":"test","data":{"data":"str1"},"more_data":{"data":"str2"}}""",
            """{"name":"test","data":[{"data":"str1"}],"more_data":[{"data":"str2"}]}""",
            """{"name":"test","data":[{"data":"str1"}],"more_data":{"data":"str2"}}"""
        )
        val goldenVal = Example("test", StringData("str1"), listOf(StringData("str2")))


        for (i in testDataInput.indices) {
            assertEquals(
                goldenVal,
                json.decodeFromString(Example.serializer(), testDataInput[i], streaming),
                "failed test on ${testDataInput[i]}, jsonTestingMode = $streaming"
            )
        }
    }

    @Test
    fun testExampleDroppingNameSerializer() = parametrizedTest { streaming ->
        val testDataInput = listOf(
            Example("First", StringData("str1")),
            Example("Second", StringData("str1"))
        )

        val goldenVals = listOf(
            """{"data":{"data":"str1"}}""",
            """{"name":"Second","data":{"data":"str1"}}"""
        )
        for (i in testDataInput.indices) {
            assertEquals(
                goldenVals[i],
                json.encodeToString(DroppingNameSerializer, testDataInput[i], streaming),
                "failed test on ${testDataInput[i]}, jsonTestingMode = $streaming"
            )
        }
    }

    @Serializable
    data class DocExample(
        @Serializable(DocJsonListSerializer::class) val data: String
    )

    object DocJsonListSerializer :
        JsonTransformingSerializer<String>(serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            if (element !is JsonArray) return element
            require(element.size == 1) { "Array size must be equal to 1 to unwrap it" }
            return element.first()
        }
    }

    @Test
    fun testDocumentationSample() = parametrizedTest { streaming ->
        val correctExample = DocExample("str1")
        assertEquals(correctExample, json.decodeFromString(DocExample.serializer(), """{"data":["str1"]}""", streaming))
        assertEquals(correctExample, json.decodeFromString(DocExample.serializer(), """{"data":"str1"}""", streaming))
    }
}
