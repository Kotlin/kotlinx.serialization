// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class BuiltinClassesTest {
    @Test
    fun testExampleBuiltin01() {
        captureOutput("ExampleBuiltin01") { example.exampleBuiltin01.main() }.verifyOutputLines(
            "{\"answer\":42,\"pi\":3.141592653589793}"
        )
    }

    @Test
    fun testExampleBuiltin02() {
        captureOutput("ExampleBuiltin02") { example.exampleBuiltin02.main() }.verifyOutputLinesStart(
            "Exception in thread \"main\" kotlinx.serialization.json.JsonEncodingException: 'NaN' is not a valid 'double' as per JSON specification.",
            "Use 'serializeSpecialFloatingPointValues = true' in 'Json {}' builder to serialize special values."
        )
    }

    @Test
    fun testExampleBuiltin03() {
        captureOutput("ExampleBuiltin03") { example.exampleBuiltin03.main() }.verifyOutputLines(
            "{\"signature\":2067120338512882656}"
        )
    }

    @Test
    fun testExampleBuiltin04() {
        captureOutput("ExampleBuiltin04") { example.exampleBuiltin04.main() }.verifyOutputLines(
            "{\"signature\":\"2067120338512882656\"}"
        )
    }

    @Test
    fun testExampleBuiltin05() {
        captureOutput("ExampleBuiltin05") { example.exampleBuiltin05.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"status\":\"SUPPORTED\"}"
        )
    }

    @Test
    fun testExampleBuiltin06() {
        captureOutput("ExampleBuiltin06") { example.exampleBuiltin06.main() }.verifyOutputLines(
            "{\"name\":\"kotlinx.serialization\",\"status\":\"maintained\"}"
        )
    }

    @Test
    fun testExampleBuiltin07() {
        captureOutput("ExampleBuiltin07") { example.exampleBuiltin07.main() }.verifyOutputLines(
            "{\"first\":1,\"second\":{\"name\":\"kotlinx.serialization\"}}"
        )
    }

    @Test
    fun testExampleBuiltin08() {
        captureOutput("ExampleBuiltin08") { example.exampleBuiltin08.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},{\"name\":\"kotlinx.coroutines\"}]"
        )
    }

    @Test
    fun testExampleBuiltin09() {
        captureOutput("ExampleBuiltin09") { example.exampleBuiltin09.main() }.verifyOutputLines(
            "[{\"name\":\"kotlinx.serialization\"},{\"name\":\"kotlinx.coroutines\"}]"
        )
    }

    @Test
    fun testExampleBuiltin10() {
        captureOutput("ExampleBuiltin10") { example.exampleBuiltin10.main() }.verifyOutputLines(
            "Data(a=[42, 42], b=[42])"
        )
    }

    @Test
    fun testExampleBuiltin11() {
        captureOutput("ExampleBuiltin11") { example.exampleBuiltin11.main() }.verifyOutputLines(
            "{\"1\":{\"name\":\"kotlinx.serialization\"},\"2\":{\"name\":\"kotlinx.coroutines\"}}"
        )
    }
}
