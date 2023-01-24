package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonNamingStrategyExclusionTest : JsonTestBase() {
    @SerialInfo
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    annotation class OriginalSerialName

    private fun List<Annotation>.hasOriginal() = filterIsInstance<OriginalSerialName>().isNotEmpty()

    private val myStrategy = JsonNamingStrategy { descriptor, index, serialName ->
        if (descriptor.annotations.hasOriginal() || descriptor.getElementAnnotations(index).hasOriginal()) serialName
        else JsonNamingStrategy.SnakeCase.serialNameForJson(descriptor, index, serialName)
    }

    @Serializable
    @OriginalSerialName
    data class Foo(val firstArg: String = "a", val secondArg: String = "b")

    enum class E {
        @OriginalSerialName
        FIRST_E,
        SECOND_E
    }

    @Serializable
    data class Bar(
        val firstBar: String = "a",
        @OriginalSerialName val secondBar: String = "b",
        val fooBar: Foo = Foo(),
        val enumBarOne: E = E.FIRST_E,
        val enumBarTwo: E = E.SECOND_E
    )

    private fun doTest(json: Json) {
        val j = Json(json) {
            namingStrategy = myStrategy
        }
        val bar = Bar()
        assertJsonFormAndRestored(
            Bar.serializer(),
            bar,
            """{"first_bar":"a","secondBar":"b","foo_bar":{"firstArg":"a","secondArg":"b"},"enum_bar_one":"FIRST_E","enum_bar_two":"SECOND_E"}""",
            j
        )
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = true
    })

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = false
    })
}
