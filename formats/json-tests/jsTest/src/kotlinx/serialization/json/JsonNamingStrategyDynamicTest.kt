package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.features.*
import kotlin.test.*

class JsonNamingStrategyDynamicTest: JsonTestBase() {
    private val jsForm = js("""{"simple":"a","one_word":"b","already_in_snake":"c","a_lot_of_words":"d","first_capitalized":"e","has_acronym_url":"BAZ","has_digit123_and_postfix":"QUX","coercion_test":"QUX"}""")
    private val jsFormNeedsCoercing = js("""{"simple":"a","one_word":"b","already_in_snake":"c","a_lot_of_words":"d","first_capitalized":"e","has_acronym_url":"BAZ","has_digit123_and_postfix":"QUX","coercion_test":"invalid"}""")

    private fun doTest(json: Json) {
        val j = Json(json) {
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
        val foo = JsonNamingStrategyTest.Foo()
        assertDynamicForm(foo)
        assertEquals(foo, j.decodeFromDynamic(jsForm))
    }

    @Test
    fun testNamingStrategyWorksWithCoercing() {
        val j = Json(default) {
            coerceInputValues = true
            useAlternativeNames = false
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
        assertEquals(JsonNamingStrategyTest.Foo(), j.decodeFromDynamic(jsFormNeedsCoercing))
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = true
    })

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = false
    })
}
