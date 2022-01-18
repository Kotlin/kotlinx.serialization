// This file was automatically generated from json.md by Knit tool. Do not edit.
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
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleJson06() {
        captureOutput("ExampleJson06") { example.exampleJson06.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\",\"website\":null}"
        )
    }

    @Test
    fun testExampleJson07() {
        captureOutput("ExampleJson07") { example.exampleJson07.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}",
            "Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)"
        )
    }

    @Test
    fun testExampleJson08() {
        captureOutput("ExampleJson08") { example.exampleJson08.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},\"Serialization\",{\"name\":\"kotlinx.coroutines\"},\"Coroutines\"]"
        )
    }

    @Test
    fun testExampleJson09() {
        captureOutput("ExampleJson09") { example.exampleJson09.main() }.verifyOutputLines(
            "{\"value\":NaN}"
        )
    }

    @Test
    fun testExampleJson10() {
        captureOutput("ExampleJson10") { example.exampleJson10.main() }.verifyOutputLines(
            "{\"#class\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExampleJson11() {
        captureOutput("ExampleJson11") { example.exampleJson11.main() }.verifyOutputLines(
            "{\"message\":{\"message_type\":\"my.app.BaseMessage\",\"message\":\"not found\"},\"error\":{\"message_type\":\"my.app.GenericError\",\"error_code\":404}}"
        )
    }

    @Test
    fun testExampleJson12() {
        captureOutput("ExampleJson12") { example.exampleJson12.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleJson13() {
        captureOutput("ExampleJson13") { example.exampleJson13.main() }.verifyOutputLines(
            "9042"
        )
    }

    @Test
    fun testExampleJson14() {
        captureOutput("ExampleJson14") { example.exampleJson14.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"owner\":{\"name\":\"kotlin\"},\"forks\":[{\"votes\":42},{\"votes\":9000}]}"
        )
    }

    @Test
    fun testExampleJson15() {
        captureOutput("ExampleJson15") { example.exampleJson15.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleJson16() {
        captureOutput("ExampleJson16") { example.exampleJson16.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, users=[User(name=kotlin)])",
            "Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])"
        )
    }

    @Test
    fun testExampleJson17() {
        captureOutput("ExampleJson17") { example.exampleJson17.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"users\":{\"name\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExampleJson18() {
        captureOutput("ExampleJson18") { example.exampleJson18.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}",
            "{\"name\":\"kotlinx.serialization\"}"
        )
    }

    @Test
    fun testExampleJson19() {
        captureOutput("ExampleJson19") { example.exampleJson19.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\"},{\"name\":\"example\"}]",
            "[OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]"
        )
    }

    @Test
    fun testExampleJson20() {
        captureOutput("ExampleJson20") { example.exampleJson20.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},{\"error\":\"Not found\"}]",
            "[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]"
        )
    }

    @Test
    fun testExampleJson21() {
        captureOutput("ExampleJson21") { example.exampleJson21.main() }.verifyOutputLines(
            "UnknownProject(name=example, details={\"type\":\"unknown\",\"maintainer\":\"Unknown\",\"license\":\"Apache 2.0\"})"
        )
    }
}
