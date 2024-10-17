// This file was automatically generated from third-party-classes.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class SerializersThirdParty {
    @Test
    fun testExampleThirdparty01() {
        captureOutput("ExampleThirdparty01") { example.exampleThirdparty01.main() }.verifyOutputLines(
            "1455494400000"
        )
    }

    @Test
    fun testExampleThirdparty02() {
        captureOutput("ExampleThirdparty02") { example.exampleThirdparty02.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleThirdparty03() {
        captureOutput("ExampleThirdparty03") { example.exampleThirdparty03.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"releaseDates\":[1688601600000,1682380800000,1672185600000]}"
        )
    }

    @Test
    fun testExampleThirdparty04() {
        captureOutput("ExampleThirdparty04") { example.exampleThirdparty04.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleThirdparty05() {
        captureOutput("ExampleThirdparty05") { example.exampleThirdparty05.main() }.verifyOutputLines(
            "{\"stableReleaseDate\":\"2016-02-15\",\"lastReleaseTimestamp\":1657152000000}"
        )
    }

    @Test
    fun testExampleThirdparty05() {
        captureOutput("ExampleThirdparty05") { example.exampleThirdparty05.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleThirdparty05() {
        captureOutput("ExampleThirdparty05") { example.exampleThirdparty05.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"stars\":9000}"
        )
    }
}
