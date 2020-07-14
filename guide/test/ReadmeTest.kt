// This file was automatically generated from README.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class ReadmeTest {
    @Test
    fun testExampleReadme01() {
        captureOutput("ExampleReadme01") { example.exampleReadme01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}",
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }
}
