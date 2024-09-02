// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class JsonTest {
    @Test
    fun testExampleJson01() {
        captureOutput("ExampleJson01") { example.exampleJson01.main() }.verifyOutputLines(
            "{",
            "    \"name\": \"kotlinx.serialization\",",
            "    \"language\": \"Kotlin\"",
            "}"
        )
    }

    @Test
    fun testExampleJson02() {
        captureOutput("ExampleJson02") { example.exampleJson02.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, status=SUPPORTED, votes=9000)"
        )
    }

    @Test
    fun testExampleJson03() {
        captureOutput("ExampleJson03") { example.exampleJson03.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization)"
        )
    }

    @Test
    fun testExampleJson04() {
        captureOutput("ExampleJson04") { example.exampleJson04.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization)",
            "Project(name=kotlinx.coroutines)"
        )
    }

    @Test
    fun testExampleJson05() {
        captureOutput("ExampleJson05") { example.exampleJson05.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\",\"website\":null}"
        )
    }

    @Test
    fun testExampleJson06() {
        captureOutput("ExampleJson06") { example.exampleJson06.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}",
            "Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)"
        )
    }

    @Test
    fun testExampleJson07() {
        captureOutput("ExampleJson07") { example.exampleJson07.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleJson08() {
        captureOutput("ExampleJson08") { example.exampleJson08.main() }.verifyOutputLines(
            "Brush(foreground=BLACK, background=null)"
        )
    }

    @Test
    fun testExampleJson09() {
        captureOutput("ExampleJson09") { example.exampleJson09.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},\"Serialization\",{\"name\":\"kotlinx.coroutines\"},\"Coroutines\"]"
        )
    }

    @Test
    fun testExampleJson10() {
        captureOutput("ExampleJson10") { example.exampleJson10.main() }.verifyOutputLines(
            "{\"value\":NaN}"
        )
    }

    @Test
    fun testExampleJson11() {
        captureOutput("ExampleJson11") { example.exampleJson11.main() }.verifyOutputLines(
            "{\"#class\":\"simple\",\"name\":\"kotlinx.serialization\"}",
            "{\"#class\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExampleJson12() {
        captureOutput("ExampleJson12") { example.exampleJson12.main() }.verifyOutputLines(
            "{\"message\":{\"message_type\":\"my.app.BaseMessage\",\"message\":\"not found\"},\"error\":{\"message_type\":\"my.app.GenericError\",\"error_code\":404}}"
        )
    }

    @Test
    fun testExampleJson13() {
        captureOutput("ExampleJson13") { example.exampleJson13.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExampleJson14() {
        captureOutput("ExampleJson14") { example.exampleJson14.main() }.verifyOutputLines(
            "CasesList(cases=[VALUE_A, VALUE_B])"
        )
    }

    @Test
    fun testExampleJson15() {
        captureOutput("ExampleJson15") { example.exampleJson15.main() }.verifyOutputLines(
            "{\"project_name\":\"kotlinx.serialization\",\"project_owner\":\"Kotlin\"}"
        )
    }
}
