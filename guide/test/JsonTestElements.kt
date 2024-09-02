// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class JsonTestElements {
    @Test
    fun testExampleJsonElements01() {
        captureOutput("ExampleJsonElements01") { example.exampleJsonElements01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleJsonElements02() {
        captureOutput("ExampleJsonElements02") { example.exampleJsonElements02.main() }.verifyOutputLines(
            "9042"
        )
    }

    @Test
    fun testExampleJsonElements03() {
        captureOutput("ExampleJsonElements03") { example.exampleJsonElements03.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"owner\":{\"name\":\"kotlin\"},\"forks\":[{\"votes\":42},{\"votes\":9000}]}"
        )
    }

    @Test
    fun testExampleJsonElements04() {
        captureOutput("ExampleJsonElements04") { example.exampleJsonElements04.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleJsonElements05() {
        captureOutput("ExampleJsonElements05") { example.exampleJsonElements05.main() }.verifyOutputLines(
            "{",
            "    \"pi_double\": 3.141592653589793,",
            "    \"pi_string\": \"3.141592653589793238462643383279\"",
            "}"
        )
    }

    @Test
    fun testExampleJsonElements06() {
        captureOutput("ExampleJsonElements06") { example.exampleJsonElements06.main() }.verifyOutputLines(
            "{",
            "    \"pi_literal\": 3.141592653589793238462643383279,",
            "    \"pi_double\": 3.141592653589793,",
            "    \"pi_string\": \"3.141592653589793238462643383279\"",
            "}"
        )
    }

    @Test
    fun testExampleJsonElements07() {
        captureOutput("ExampleJsonElements07") { example.exampleJsonElements07.main() }.verifyOutputLines(
            "3.141592653589793238462643383279"
        )
    }

    @Test
    fun testExampleJsonElements08() {
        captureOutput("ExampleJsonElements08") { example.exampleJsonElements08.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonEncodingException: Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive"
        )
    }
}
