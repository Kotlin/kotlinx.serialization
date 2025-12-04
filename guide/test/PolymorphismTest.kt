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
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly03() {
        captureOutput("ExamplePoly03") { example.examplePoly03.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for subclass 'OwnedProject' is not found in the polymorphic scope of 'Project'.",
            "Check if class with serial name 'OwnedProject' exists and serializer is registered in a corresponding SerializersModule.",
            "To be registered automatically, class 'OwnedProject' has to be '@Serializable', and the base class 'Project' has to be sealed and '@Serializable'."
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
            "{\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly06() {
        captureOutput("ExamplePoly06") { example.examplePoly06.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly07() {
        captureOutput("ExamplePoly07") { example.examplePoly07.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"status\":\"open\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly08() {
        captureOutput("ExamplePoly08") { example.examplePoly08.main() }.verifyOutputLines(
            "[{\"type\":\"example.examplePoly08.EmptyResponse\"},{\"type\":\"example.examplePoly08.TextResponse\",\"text\":\"OK\"}]"
        )
    }

    @Test
    fun testExamplePoly09() {
        captureOutput("ExamplePoly09") { example.examplePoly09.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly10() {
        captureOutput("ExamplePoly10") { example.examplePoly10.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly11() {
        captureOutput("ExamplePoly11") { example.examplePoly11.main() }.verifyOutputLinesStart(
            "{\"type\":\"example.examplePoly11.Sub1\",\"data\":\"kotlin\"}",
            "{\"type\":\"example.examplePoly11.Sub1\",\"data\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly12() {
        captureOutput("ExamplePoly12") { example.examplePoly12.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly13() {
        captureOutput("ExamplePoly13") { example.examplePoly13.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly14() {
        captureOutput("ExamplePoly14") { example.examplePoly14.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly15() {
        captureOutput("ExamplePoly15") { example.examplePoly15.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly16() {
        captureOutput("ExamplePoly16") { example.examplePoly16.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly17() {
        captureOutput("ExamplePoly17") { example.examplePoly17.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"},\"any\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly18() {
        captureOutput("ExamplePoly18") { example.examplePoly18.main() }.verifyOutputLines(
            "{\"type\":\"OkResponse\",\"data\":{\"type\":\"OwnedProject\",\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\"}}",
            "OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))"
        )
    }

    @Test
    fun testExamplePoly19() {
        captureOutput("ExamplePoly19") { example.examplePoly19.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 0: Serializer for subclass 'unknown' is not found in the polymorphic scope of 'Project' at path: $",
            "Check if class with serial name 'unknown' exists and serializer is registered in a corresponding SerializersModule."
        )
    }

    @Test
    fun testExamplePoly20() {
        captureOutput("ExamplePoly20") { example.examplePoly20.main() }.verifyOutputLines(
            "[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]"
        )
    }

    @Test
    fun testExamplePoly21() {
        captureOutput("ExamplePoly21") { example.examplePoly21.main() }.verifyOutputLines(
            "{\"type\":\"Cat\",\"catType\":\"Tabby\"}"
        )
    }
}
