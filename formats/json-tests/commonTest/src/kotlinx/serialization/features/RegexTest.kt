package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test

class RegexTest : JsonTestBase() {
//    TODO uncomment when compiler plugin is released
//    PR: https://github.com/JetBrains/kotlin/pull/5060
//    @Serializable
//    data class RegexHolder(val regex: Regex)
//    @Test
//    fun testRegex() {
//        val pattern = "^(.+)@(\\\\S+)\$"
//        assertJsonFormAndRestored(
//            RegexHolder.serializer(),
//            RegexHolder(pattern.toRegex()),
//            """{"regex": "$pattern""""
//        )
//    }
}
