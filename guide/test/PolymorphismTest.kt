// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class PolymorphismTest {
    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.coroutines\"}"
        )
    }

    @Test
    fun testExamplePoly02() {
        captureOutput("ExamplePoly02") { example.examplePoly02.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'OwnedProject' is not found.",
            "Mark the class as @Serializable or provide the serializer explicitly."
        )
    }

    @Test
    fun testExamplePoly03() {
        captureOutput("ExamplePoly03") { example.examplePoly03.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Class 'OwnedProject' is not registered for polymorphic serialization in the scope of 'Project'.",
            "Mark the base class as 'sealed' or register the serializer explicitly."
        )
    }

    @Test
    fun testExamplePoly04() {
        captureOutput("ExamplePoly04") { example.examplePoly04.main() }.verifyOutputLines(
            "{\"type\":\"example.examplePoly04.OwnedProject\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly05() {
        captureOutput("ExamplePoly05") { example.examplePoly05.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly06() {
        captureOutput("ExamplePoly06") { example.examplePoly06.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"status\":\"open\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly07() {
        captureOutput("ExamplePoly07") { example.examplePoly07.main() }.verifyOutputLines(
            "[{\"type\":\"example.examplePoly07.EmptyResponse\"},{\"type\":\"example.examplePoly07.TextResponse\",\"text\":\"OK\"}]"
        )
    }

    @Test
    fun testExamplePoly08() {
        captureOutput("ExamplePoly08") { example.examplePoly08.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly09() {
        captureOutput("ExamplePoly09") { example.examplePoly09.main() }.verifyOutputLinesStart(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly10() {
        captureOutput("ExamplePoly10") { example.examplePoly10.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly11() {
        captureOutput("ExamplePoly11") { example.examplePoly11.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Mark the class as @Serializable or provide the serializer explicitly."
        )
    }

    @Test
    fun testExamplePoly12() {
        captureOutput("ExamplePoly12") { example.examplePoly12.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Mark the class as @Serializable or provide the serializer explicitly."
        )
    }

    @Test
    fun testExamplePoly13() {
        captureOutput("ExamplePoly13") { example.examplePoly13.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly14() {
        captureOutput("ExamplePoly14") { example.examplePoly14.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly15() {
        captureOutput("ExamplePoly15") { example.examplePoly15.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"},\"any\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly16() {
        captureOutput("ExamplePoly16") { example.examplePoly16.main() }.verifyOutputLines(
            "{\"type\":\"OkResponse\",\"data\":{\"type\":\"OwnedProject\",\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\"}}",
            "OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))"
        )
    }

    @Test
    fun testExamplePoly17() {
        captureOutput("ExamplePoly17") { example.examplePoly17.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonDecodingException: Polymorphic serializer was not found for class discriminator 'unknown'"
        )
    }

    @Test
    fun testExamplePoly18() {
        captureOutput("ExamplePoly18") { example.examplePoly18.main() }.verifyOutputLines(
            "[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]"
        )
    }
}
