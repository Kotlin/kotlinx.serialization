// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class BasicSerializationTest {
    @Test
    fun testExampleBasic01() {
        captureOutput("ExampleBasic01") { example.exampleBasic01.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.SerializationException: Serializer for class 'Project' is not found.",
            "Mark the class as @Serializable or provide the serializer explicitly."
        )
    }

    @Test
    fun testExampleBasic02() {
        captureOutput("ExampleBasic02") { example.exampleBasic02.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}"
        )
    }

    @Test
    fun testExampleBasic03() {
        captureOutput("ExampleBasic03") { example.exampleBasic03.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleClasses01() {
        captureOutput("ExampleClasses01") { example.exampleClasses01.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"stars\":9000}"
        )
    }

    @Test
    fun testExampleClasses02() {
        captureOutput("ExampleClasses02") { example.exampleClasses02.main() }.verifyOutputLines(
            "{\"owner\":\"kotlin\",\"name\":\"kotlinx.serialization\"}"
        )
    }

    @Test
    fun testExampleClasses03() {
        captureOutput("ExampleClasses03") { example.exampleClasses03.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" java.lang.IllegalArgumentException: name cannot be empty"
        )
    }

    @Test
    fun testExampleClasses04() {
        captureOutput("ExampleClasses04") { example.exampleClasses04.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.MissingFieldException: Field 'language' is required for type with serial name 'example.exampleClasses04.Project', but it was missing at path: $"
        )
    }

    @Test
    fun testExampleClasses05() {
        captureOutput("ExampleClasses05") { example.exampleClasses05.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleClasses06() {
        captureOutput("ExampleClasses06") { example.exampleClasses06.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleClasses07() {
        captureOutput("ExampleClasses07") { example.exampleClasses07.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.MissingFieldException: Field 'language' is required for type with serial name 'example.exampleClasses07.Project', but it was missing at path: $"
        )
    }

    @Test
    fun testExampleClasses08() {
        captureOutput("ExampleClasses08") { example.exampleClasses08.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 42: Encountered an unknown key 'language' at path: $.name",
            "Use 'ignoreUnknownKeys = true' in 'Json {}' builder to ignore unknown keys."
        )
    }

    @Test
    fun testExampleClasses09() {
        captureOutput("ExampleClasses09") { example.exampleClasses09.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\"}"
        )
    }

    @Test
    fun testExampleClasses10() {
        captureOutput("ExampleClasses10") { example.exampleClasses10.main() }.verifyOutputLines(
            "{\"name\":\"Alice\",\"projects\":[{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}]}",
            "{\"name\":\"Bob\"}"
        )
    }

    @Test
    fun testExampleClasses11() {
        captureOutput("ExampleClasses11") { example.exampleClasses11.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\"}"
        )
    }

    @Test
    fun testExampleClasses12() {
        captureOutput("ExampleClasses12") { example.exampleClasses12.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 52: Expected string literal but 'null' literal was found at path: $.language",
            "Use 'coerceInputValues = true' in 'Json {}` builder to coerce nulls to default values."
        )
    }

    @Test
    fun testExampleClasses13() {
        captureOutput("ExampleClasses13") { example.exampleClasses13.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"owner\":{\"name\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExampleClasses14() {
        captureOutput("ExampleClasses14") { example.exampleClasses14.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"owner\":{\"name\":\"kotlin\"},\"maintainer\":{\"name\":\"kotlin\"}}"
        )
    }

    @Test
    fun testExampleClasses15() {
        captureOutput("ExampleClasses15") { example.exampleClasses15.main() }.verifyOutputLines(
            "{\"a\":{\"contents\":42},\"b\":{\"contents\":{\"name\":\"kotlinx.serialization\",\"language\":\"Kotlin\"}}}"
        )
    }

    @Test
    fun testExampleClasses16() {
        captureOutput("ExampleClasses16") { example.exampleClasses16.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"lang\":\"Kotlin\"}"
        )
    }
}
