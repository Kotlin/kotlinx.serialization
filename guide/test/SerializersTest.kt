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
    fun testExampleSerializer02() {
        captureOutput("ExampleSerializer02") { example.exampleSerializer02.main() }.verifyOutputLines(
            "Color(rgb: kotlin.Int)"
        )
    }

    @Test
    fun testExampleSerializer03() {
        captureOutput("ExampleSerializer03") { example.exampleSerializer03.main() }.verifyOutputLines(
            "Box(contents: Color)"
        )
    }

    @Test
    fun testExampleSerializer04() {
        captureOutput("ExampleSerializer04") { example.exampleSerializer04.main() }.verifyOutputLines(
            "PrimitiveDescriptor(kotlin.Int)"
        )
    }

    @Test
    fun testExampleSerializer05() {
        captureOutput("ExampleSerializer05") { example.exampleSerializer05.main() }.verifyOutputLines(
            "kotlin.collections.ArrayList(PrimitiveDescriptor(kotlin.String))"
        )
    }

    @Test
    fun testExampleSerializer06() {
        captureOutput("ExampleSerializer06") { example.exampleSerializer06.main() }.verifyOutputLines(
            "kotlin.collections.LinkedHashMap(PrimitiveDescriptor(kotlin.String), Color(rgb: kotlin.Int))"
        )
    }

    @Test
    fun testExampleSerializer07() {
        captureOutput("ExampleSerializer07") { example.exampleSerializer07.main() }.verifyOutputLines(
            "\"00ff00\""
        )
    }

    @Test
    fun testExampleSerializer08() {
        captureOutput("ExampleSerializer08") { example.exampleSerializer08.main() }.verifyOutputLines(
            "65280"
        )
    }

    @Test
    fun testExampleSerializer09() {
        captureOutput("ExampleSerializer09") { example.exampleSerializer09.main() }.verifyOutputLines(
            "{\"background\":\"ffffff\",\"foreground\":\"000000\"}"
        )
    }

    @Test
    fun testExampleSerializer10() {
        captureOutput("ExampleSerializer10") { example.exampleSerializer10.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer11() {
        captureOutput("ExampleSerializer11") { example.exampleSerializer11.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer12() {
        captureOutput("ExampleSerializer12") { example.exampleSerializer12.main() }.verifyOutputLines(
            "{\"r\":0,\"g\":255,\"b\":0}"
        )
    }

    @Test
    fun testExampleSerializer13() {
        captureOutput("ExampleSerializer13") { example.exampleSerializer13.main() }.verifyOutputLines(
            "1455494400000"
        )
    }

    @Test
    fun testExampleSerializer14() {
        captureOutput("ExampleSerializer14") { example.exampleSerializer14.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer15() {
        captureOutput("ExampleSerializer15") { example.exampleSerializer15.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer16() {
        captureOutput("ExampleSerializer16") { example.exampleSerializer16.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\"}",
            "Box(contents=Project(name=kotlinx.serialization))"
        )
    }

    @Test
    fun testExampleSerializer17() {
        captureOutput("ExampleSerializer17") { example.exampleSerializer17.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Date' is not found.",
            "Mark the class as @Serializable or provide the serializer explicitly."
        )
    }

    @Test
    fun testExampleSerializer18() {
        captureOutput("ExampleSerializer18") { example.exampleSerializer18.main() }.verifyOutputLines(
            "{\"name\":\"Kotlin\",\"stableReleaseDate\":1455494400000}"
        )
    }

    @Test
    fun testExampleSerializer19() {
        captureOutput("ExampleSerializer19") { example.exampleSerializer19.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleSerializer20() {
        captureOutput("ExampleSerializer20") { example.exampleSerializer20.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"stars\":9000}"
        )
    }
}
