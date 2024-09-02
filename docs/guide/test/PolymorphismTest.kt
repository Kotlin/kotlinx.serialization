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
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'OwnedProject' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for subclass 'OwnedProject' is not found in the polymorphic scope of 'Project'.",
            "Check if class with serial name 'OwnedProject' exists and serializer is registered in a corresponding SerializersModule.",
            "To be registered automatically, class 'OwnedProject' has to be '@Serializable', and the base class 'Project' has to be sealed and '@Serializable'."
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"example.examplePoly04.OwnedProject\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"status\":\"open\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "[{\"type\":\"example.examplePoly08.EmptyResponse\"},{\"type\":\"example.examplePoly08.TextResponse\",\"text\":\"OK\"}]"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.",
            "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied."
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"project\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"},\"any\":{\"type\":\"owned\",\"name\":\"kotlinx.coroutines\",\"owner\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"OkResponse\",\"data\":{\"type\":\"OwnedProject\",\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\"}}",
            "OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 0: Serializer for subclass 'unknown' is not found in the polymorphic scope of 'Project' at path: $",
            "Check if class with serial name 'unknown' exists and serializer is registered in a corresponding SerializersModule."
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]"
        )
    }

    @Test
    fun testExamplePoly01() {
        captureOutput("ExamplePoly01") { example.examplePoly01.main() }.verifyOutputLines(
            "{\"type\":\"Cat\",\"catType\":\"Tabby\"}"
        )
    }
}
