// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.test

import org.junit.Test
import kotlinx.knit.test.*

class FormatsTest {
    @Test
    fun testExampleFormats01() {
        captureOutput("ExampleFormats01") { example.exampleFormats01.main() }.verifyOutputLines(
            "{BF}dnameukotlinx.serializationhlanguagefKotlin{FF}",
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleFormats02() {
        captureOutput("ExampleFormats02") { example.exampleFormats02.main() }.verifyOutputLines(
            "Project(name=kotlinx.serialization)"
        )
    }

    @Test
    fun testExampleFormats03() {
        captureOutput("ExampleFormats03") { example.exampleFormats03.main() }.verifyOutputLines(
            "{BF}etype2D{01}{02}{03}{04}etype4{9F}{05}{06}{07}{08}{FF}{FF}",
            "Data(type2=[1, 2, 3, 4], type4=[5, 6, 7, 8])"
        )
    }

    @Test
    fun testExampleFormats04() {
        captureOutput("ExampleFormats04") { example.exampleFormats04.main() }.verifyOutputLines(
            "{0A}{15}kotlinx.serialization{12}{06}Kotlin",
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleFormats05() {
        captureOutput("ExampleFormats05") { example.exampleFormats05.main() }.verifyOutputLines(
            "{0A}{15}kotlinx.serialization{1A}{06}Kotlin",
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleFormats06() {
        captureOutput("ExampleFormats06") { example.exampleFormats06.main() }.verifyOutputLines(
            "{08}{01}{10}{03}{1D}{03}{00}{00}{00}"
        )
    }

    @Test
    fun testExampleFormats07() {
        captureOutput("ExampleFormats07") { example.exampleFormats07.main() }.verifyOutputLines(
            "{08}{01}{08}{02}{08}{03}",
            "Data(a=[1, 2, 3], b=[])"
        )
    }

    @Test
    fun testExampleFormats08() {
        captureOutput("ExampleFormats08") { example.exampleFormats08.main() }.verifyOutputLines(
            "syntax = \"proto2\";",
            "",
            "",
            "// serial name 'example.exampleFormats08.SampleData'",
            "message SampleData {",
            "  required int64 amount = 1;",
            "  optional string description = 2;",
            "  // WARNING: a default value decoded when value is missing",
            "  optional string department = 3;",
            "}",
            ""
        )
    }

    @Test
    fun testExampleFormats09() {
        captureOutput("ExampleFormats09") { example.exampleFormats09.main() }.verifyOutputLines(
            "name = kotlinx.serialization",
            "owner.name = kotlin"
        )
    }

    @Test
    fun testExampleFormats10() {
        captureOutput("ExampleFormats10") { example.exampleFormats10.main() }.verifyOutputLines(
            "[kotlinx.serialization, kotlin, 9000]"
        )
    }

    @Test
    fun testExampleFormats11() {
        captureOutput("ExampleFormats11") { example.exampleFormats11.main() }.verifyOutputLines(
            "[kotlinx.serialization, kotlin, 9000]",
            "Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)"
        )
    }

    @Test
    fun testExampleFormats12() {
        captureOutput("ExampleFormats12") { example.exampleFormats12.main() }.verifyOutputLines(
            "[kotlinx.serialization, kotlin, 9000]",
            "Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)"
        )
    }

    @Test
    fun testExampleFormats13() {
        captureOutput("ExampleFormats13") { example.exampleFormats13.main() }.verifyOutputLines(
            "[kotlinx.serialization, 2, kotlin, jetbrains, 9000]",
            "Project(name=kotlinx.serialization, owners=[User(name=kotlin), User(name=jetbrains)], votes=9000)"
        )
    }

    @Test
    fun testExampleFormats14() {
        captureOutput("ExampleFormats14") { example.exampleFormats14.main() }.verifyOutputLines(
            "[kotlinx.serialization, !!, kotlin, NULL]",
            "Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=null)"
        )
    }

    @Test
    fun testExampleFormats15() {
        captureOutput("ExampleFormats15") { example.exampleFormats15.main() }.verifyOutputLines(
            "{00}{15}kotlinx.serialization{00}{06}Kotlin",
            "Project(name=kotlinx.serialization, language=Kotlin)"
        )
    }

    @Test
    fun testExampleFormats16() {
        captureOutput("ExampleFormats16") { example.exampleFormats16.main() }.verifyOutputLines(
            "{00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}",
            "Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])"
        )
    }
}
