package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

@Suppress("EnumEntryName")
class JsonEnumsCaseInsensitiveTest: JsonTestBase() {
    @Serializable
    data class Foo(
        val one: Bar = Bar.BAZ,
        val two: Bar = Bar.QUX,
        val three: Bar = Bar.QUX
    )

    enum class Bar { BAZ, QUX }

    // It seems that we no longer report a warning that @Serializable is required for enums with @SerialName.
    // It is still required for them to work at top-level.
    @Serializable
    enum class Cases {
        ALL_CAPS,
        MiXed,
        all_lower,

        @JsonNames("AltName")
        hasAltNames,

        @SerialName("SERIAL_NAME")
        hasSerialName
    }

    @Serializable
    data class EnumCases(val cases: List<Cases>)

    val json = Json(default) { decodeEnumsCaseInsensitive = true }

    @Test
    fun testCases() = parametrizedTest { mode ->
        val input =
            """{"cases":["ALL_CAPS","all_caps","mixed","MIXED","miXed","all_lower","ALL_LOWER","all_Lower","hasAltNames","HASALTNAMES","altname","ALTNAME","AltName","SERIAL_NAME","serial_name"]}"""
        val target = listOf(
            Cases.ALL_CAPS,
            Cases.ALL_CAPS,
            Cases.MiXed,
            Cases.MiXed,
            Cases.MiXed,
            Cases.all_lower,
            Cases.all_lower,
            Cases.all_lower,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasSerialName,
            Cases.hasSerialName
        )
        val decoded = json.decodeFromString<EnumCases>(input, mode)
        assertEquals(EnumCases(target), decoded)
        val encoded = json.encodeToString(decoded, mode)
        assertEquals(
            """{"cases":["ALL_CAPS","ALL_CAPS","MiXed","MiXed","MiXed","all_lower","all_lower","all_lower","hasAltNames","hasAltNames","hasAltNames","hasAltNames","hasAltNames","SERIAL_NAME","SERIAL_NAME"]}""",
            encoded
        )
    }

    @Test
    fun testTopLevelList() = parametrizedTest { mode ->
        val input = """["all_caps","serial_name"]"""
        val decoded = json.decodeFromString<List<Cases>>(input, mode)
        assertEquals(listOf(Cases.ALL_CAPS, Cases.hasSerialName), decoded)
        assertEquals("""["ALL_CAPS","SERIAL_NAME"]""", json.encodeToString(decoded, mode))
    }

    @Test
    fun testTopLevelEnum() = parametrizedTest { mode ->
        val input = """"altName""""
        val decoded = json.decodeFromString<Cases>(input, mode)
        assertEquals(Cases.hasAltNames, decoded)
        assertEquals(""""hasAltNames"""", json.encodeToString(decoded, mode))
    }

    @Test
    fun testSimpleCase() = parametrizedTest { mode ->
        val input = """{"one":"baz","two":"Qux","three":"QUX"}"""
        val decoded = json.decodeFromString<Foo>(input, mode)
        assertEquals(Foo(), decoded)
        assertEquals("""{"one":"BAZ","two":"QUX","three":"QUX"}""", json.encodeToString(decoded, mode))
    }

    enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }

    @Test
    fun testDocSample() {

        val j = Json { decodeEnumsCaseInsensitive = true }
        @Serializable
        data class Outer(val enums: List<E>)

        println(j.decodeFromString<Outer>("""{"enums":["value_A", "alternative"]}""").enums)
    }

    @Test
    fun testCoercingStillWorks() = parametrizedTest { mode ->
        val withCoercing = Json(json) { coerceInputValues = true }
        val input = """{"one":"baz","two":"unknown","three":"Que"}"""
        assertEquals(Foo(),  withCoercing.decodeFromString<Foo>(input, mode))
    }

    @Test
    fun testCaseInsensitivePriorityOverCoercing() = parametrizedTest { mode ->
        val withCoercing = Json(json) { coerceInputValues = true }
        val input = """{"one":"QuX","two":"Baz","three":"Que"}"""
        assertEquals(Foo(Bar.QUX, Bar.BAZ, Bar.QUX),  withCoercing.decodeFromString<Foo>(input, mode))
    }

    @Test
    fun testCoercingStillWorksWithNulls() = parametrizedTest { mode ->
        val withCoercing = Json(json) { coerceInputValues = true }
        val input = """{"one":"baz","two":"null","three":null}"""
        assertEquals(Foo(),  withCoercing.decodeFromString<Foo>(input, mode))
    }

    @Test
    fun testFeatureDisablesProperly() = parametrizedTest { mode ->
        val disabled = Json(json) {
            coerceInputValues = true
            decodeEnumsCaseInsensitive = false
        }
        val input = """{"one":"BAZ","two":"BAz","three":"baz"}""" // two and three should be coerced to QUX
        assertEquals(Foo(), disabled.decodeFromString<Foo>(input, mode))
    }

    @Test
    fun testFeatureDisabledThrowsWithoutCoercing() = parametrizedTest { mode ->
        val disabled = Json(json) {
            coerceInputValues = false
            decodeEnumsCaseInsensitive = false
        }
        val input = """{"one":"BAZ","two":"BAz","three":"baz"}"""
        assertFailsWithMessage<SerializationException>("does not contain element with name 'BAz'") {
            disabled.decodeFromString<Foo>(input, mode)
        }
    }

    @Serializable enum class BadEnum { Bad, BAD }

    @Serializable data class ListBadEnum(val l: List<BadEnum>)

    @Test
    fun testLowercaseClashThrowsException() = parametrizedTest { mode ->
        assertFailsWithMessage<SerializationException>("""The suggested name 'bad' for enum value BAD is already one of the names for enum value Bad""") {
            json.decodeFromString<Box<BadEnum>>("""{"boxed":"bad"}""", mode)
        }
        assertFailsWithMessage<SerializationException>("""The suggested name 'bad' for enum value BAD is already one of the names for enum value Bad""") {
            json.decodeFromString<Box<BadEnum>>("""{"boxed":"unrelated"}""", mode)
        }
    }

    @Test
    fun testLowercaseClashHandledWithoutFeature() = parametrizedTest { mode ->
        val disabled = Json(json) {
            coerceInputValues = false
            decodeEnumsCaseInsensitive = false
        }
        assertEquals(ListBadEnum(listOf(BadEnum.Bad, BadEnum.BAD)), disabled.decodeFromString("""{"l":["Bad","BAD"]}""", mode))
    }
}
