// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class JsonTest {
    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "9042"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"owner\":{\"name\":\"kotlin\"},\"forks\":[{\"votes\":42},{\"votes\":9000}]}"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "{",
            "    \"pi_double\": 3.141592653589793,",
            "    \"pi_string\": \"3.141592653589793238462643383279\"",
            "}"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "{",
            "    \"pi_literal\": 3.141592653589793238462643383279,",
            "    \"pi_double\": 3.141592653589793,",
            "    \"pi_string\": \"3.141592653589793238462643383279\"",
            "}"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "3.141592653589793238462643383279"
        )
    }

    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonEncodingException: Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive"
        )
    }
}
