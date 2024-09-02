// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class JsonTestTransform {
    @Test
    fun testExampleJsonTransform01() {
        captureOutput("ExampleJsonTransform01") { example.exampleJsonTransform01.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, users=[User(name=kotlin)])",
            "Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])"
        )
    }

    @Test
    fun testExampleJsonTransform02() {
        captureOutput("ExampleJsonTransform02") { example.exampleJsonTransform02.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"users\":{\"name\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExampleJsonTransform03() {
        captureOutput("ExampleJsonTransform03") { example.exampleJsonTransform03.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}",
            "{\"name\":\"kotlinx.serialization\"}"
        )
    }

    @Test
    fun testExampleJsonTransform04() {
        captureOutput("ExampleJsonTransform04") { example.exampleJsonTransform04.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\"},{\"name\":\"example\"}]",
            "[OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]"
        )
    }

    @Test
    fun testExampleJsonTransform05() {
        captureOutput("ExampleJsonTransform05") { example.exampleJsonTransform05.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},{\"error\":\"Not found\"}]",
            "[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]"
        )
    }

    @Test
    fun testExampleJsonTransform06() {
        captureOutput("ExampleJsonTransform06") { example.exampleJsonTransform06.main() }.verifyOutputLines(
            "UnknownProject(name=example, details={\"type\":\"unknown\",\"maintainer\":\"Unknown\",\"license\":\"Apache 2.0\"})"
        )
    }
}
