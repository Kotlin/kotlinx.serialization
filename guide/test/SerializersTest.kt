// This file was automatically generated from create-custom-serializers.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class SerializersTest {
    @Test
    fun testExampleSerializer01() {
        captureOutput("ExampleSerializer01") { example.exampleSerializer01.main() }.verifyOutputLines(
            "Color(rgb: kotlin.Int)"
        )
    }

    @Test
    fun testExampleSerializer02() {
        captureOutput("ExampleSerializer02") { example.exampleSerializer02.main() }.verifyOutputLines(
            "\"00ff00\"",
            "65280"
        )
    }

    @Test
    fun testExampleSerializer03() {
        captureOutput("ExampleSerializer03") { example.exampleSerializer03.main() }.verifyOutputLines(
            "[0,255,0]"
        )
    }

    @Test
    fun testExampleSerializer04() {
        captureOutput("ExampleSerializer04") { example.exampleSerializer04.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer05() {
        captureOutput("ExampleSerializer05") { example.exampleSerializer05.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer06() {
        captureOutput("ExampleSerializer06") { example.exampleSerializer06.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer07() {
        captureOutput("ExampleSerializer07") { example.exampleSerializer07.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\"}",
            "Box(contents=Project(name=kotlinx.serialization))"
        )
    }

    @Test
    fun testExampleSerializer08() {
        captureOutput("ExampleSerializer08") { example.exampleSerializer08.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }
}
