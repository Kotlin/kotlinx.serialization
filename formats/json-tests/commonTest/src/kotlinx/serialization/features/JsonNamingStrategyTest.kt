package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*


class JsonNamingStrategyTest : JsonTestBase() {
    @Serializable
    data class Foo(
        val simple: String = "a",
        val oneWord: String = "b",
        val already_in_snake: String = "c",
        val aLotOfWords: String = "d",
        val FirstCapitalized: String = "e",
        val hasAcronymURL: Bar = Bar.BAZ,
        val hasDigit123AndPostfix: Bar = Bar.QUX,
        val coercionTest: Bar = Bar.QUX
    )

    enum class Bar { BAZ, QUX }

    val jsonWithNaming = Json(default) {
        namingStrategy = JsonNamingStrategy.SnakeCase
        decodeEnumsCaseInsensitive = true // check that related feature does not break anything
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() = doTest(Json(jsonWithNaming) {
        useAlternativeNames = true
    })

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() = doTest(Json(jsonWithNaming) {
        useAlternativeNames = false
    })

    private fun doTest(json: Json) {
        val foo = Foo()
        assertJsonFormAndRestored(
            Foo.serializer(),
            foo,
            """{"simple":"a","one_word":"b","already_in_snake":"c","a_lot_of_words":"d","first_capitalized":"e","has_acronym_url":"BAZ","has_digit123_and_postfix":"QUX","coercion_test":"QUX"}""",
            json
        )
    }

    @Test
    fun testNamingStrategyWorksWithCoercing() {
        val j = Json(jsonWithNaming) {
            coerceInputValues = true
            useAlternativeNames = false
        }
        assertEquals(
            Foo(),
            j.decodeFromString("""{"simple":"a","one_word":"b","already_in_snake":"c","a_lot_of_words":"d","first_capitalized":"e","has_acronym_url":"baz","has_digit123_and_postfix":"qux","coercion_test":"invalid"}""")
        )
    }

    @Test
    fun testSnakeCaseStrategy() {
        fun apply(name: String) =
            JsonNamingStrategy.SnakeCase.serialNameForJson(String.serializer().descriptor, 0, name)

        val cases = mapOf<String, String>(
            "" to "",
            "_" to "_",
            "___" to "___",
            "a" to "a",
            "A" to "a",
            "_1" to "_1",
            "_a" to "_a",
            "_A" to "_a",
            "property" to "property",
            "twoWords" to "two_words",
            "threeDistinctWords" to "three_distinct_words",
            "ThreeDistinctWords" to "three_distinct_words",
            "Oneword" to "oneword",
            "camel_Case_Underscores" to "camel_case_underscores",
            "_many____underscores__" to "_many____underscores__",
            "URLmapping" to "ur_lmapping",
            "URLMapping" to "url_mapping",
            "IOStream" to "io_stream",
            "IOstream" to "i_ostream",
            "myIo2Stream" to "my_io2_stream",
            "myIO2Stream" to "my_io2_stream",
            "myIO2stream" to "my_io2stream",
            "myIO2streamMax" to "my_io2stream_max",
            "InURLBetween" to "in_url_between",
            "myHTTP2APIKey" to "my_http2_api_key",
            "myHTTP2fastApiKey" to "my_http2fast_api_key",
            "myHTTP23APIKey" to "my_http23_api_key",
            "myHttp23ApiKey" to "my_http23_api_key",
            "theWWW" to "the_www",
            "theWWW_URL_xxx" to "the_www_url_xxx",
            "hasDigit123AndPostfix" to "has_digit123_and_postfix"
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, apply(input))
        }
    }

    @Serializable
    data class DontUseOriginal(val testCase: String)

    @Test
    fun testNamingStrategyOverridesOriginal() {
        val json = Json(jsonWithNaming) {
            ignoreUnknownKeys = true
        }
        parametrizedTest { mode ->
            assertEquals(DontUseOriginal("a"), json.decodeFromString("""{"test_case":"a","testCase":"b"}""", mode))
        }

        val jsonThrows = Json(jsonWithNaming) {
            ignoreUnknownKeys = false
        }
        parametrizedTest { mode ->
            assertFailsWithMessage<SerializationException>("Encountered an unknown key 'testCase'") {
                jsonThrows.decodeFromString<DontUseOriginal>("""{"test_case":"a","testCase":"b"}""", mode)
            }
        }
    }

    @Serializable
    data class CollisionCheckPrimary(val testCase: String, val test_case: String)

    @Serializable
    data class CollisionCheckAlternate(val testCase: String, @JsonNames("test_case") val testCase2: String)

    @Test
    fun testNamingStrategyPrioritizesOverAlternative() {
        val json = Json(jsonWithNaming) {
            ignoreUnknownKeys = true
        }
        parametrizedTest { mode ->
            assertFailsWithMessage<SerializationException>("The suggested name 'test_case' for property test_case is already one of the names for property testCase") {
                json.decodeFromString<CollisionCheckPrimary>("""{"test_case":"a"}""", mode)
            }
        }
        parametrizedTest { mode ->
            assertFailsWithMessage<SerializationException>("The suggested name 'test_case' for property testCase2 is already one of the names for property testCase") {
                json.decodeFromString<CollisionCheckAlternate>("""{"test_case":"a"}""", mode)
            }
        }
    }


    @Serializable
    data class OriginalAsFallback(@JsonNames("testCase") val testCase: String)

    @Test
    fun testCanUseOriginalNameAsAlternative() {
        val json = Json(jsonWithNaming) {
            ignoreUnknownKeys = true
        }
        parametrizedTest { mode ->
            assertEquals(OriginalAsFallback("b"), json.decodeFromString("""{"testCase":"b"}""", mode))
        }
    }

    @Serializable
    sealed interface SealedBase {
        @Serializable
        @JsonClassDiscriminator("typeSub")
        sealed class SealedMid : SealedBase {
            @Serializable
            @SerialName("SealedSub1")
            object SealedSub1 : SealedMid()
        }

        @Serializable
        @SerialName("SealedSub2")
        data class SealedSub2(val testCase: Int = 0) : SealedBase
    }

    @Serializable
    data class Holder(val testBase: SealedBase, val testMid: SealedBase.SealedMid)

    @Test
    fun testNamingStrategyDoesNotAffectPolymorphism() {
        val json = Json(jsonWithNaming) {
            classDiscriminator = "typeBase"
        }
        val holder = Holder(SealedBase.SealedSub2(), SealedBase.SealedMid.SealedSub1)
        assertJsonFormAndRestored(
            Holder.serializer(),
            holder,
            """{"test_base":{"typeBase":"SealedSub2","test_case":0},"test_mid":{"typeSub":"SealedSub1"}}""",
            json
        )
    }
}
