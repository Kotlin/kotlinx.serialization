// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class SerializersTest {
    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"rgb\":65280}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "Color(rgb: kotlin.Int)"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "Box(contents: Color)"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "PrimitiveDescriptor(kotlin.Int)"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "kotlin.collections.ArrayList(PrimitiveDescriptor(kotlin.String))"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "kotlin.collections.LinkedHashMap(PrimitiveDescriptor(kotlin.String), Color(rgb: kotlin.Int))"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "\"00ff00\""
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "65280"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"background\":\"ffffff\",\"foreground\":\"000000\"}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "[0,255,0]"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "1455494400000"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"releaseDates\":[1688601600000,1682380800000,1672185600000]}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"stableReleaseDate\":\"2016-02-15\",\"lastReleaseTimestamp\":1657152000000}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\"}",
            "Box(contents=Project(name=kotlinx.serialization))"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Date' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"stars\":9000}"
        )
    }
}
