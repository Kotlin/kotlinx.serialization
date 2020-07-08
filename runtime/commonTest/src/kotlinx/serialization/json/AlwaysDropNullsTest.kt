/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class AlwaysDropNullsTest : JsonTestBase() {
    @Serializable
    data class NullableData(val i: Int, val s1: String?, val s2: String? = null)

    @Serializable
    data class NullableInList(val l: List<String?>)

    @Serializable
    data class NullableInMap(val m: Map<Int, String?>)

    @Test
    fun testNullsDropped() {
        val subject = NullableData(42, null)
        parametrizedTest(Json(JsonConfiguration.Default.copy(encodeDefaults = true, alwaysDropNulls = true))) {
            assertEquals("""{"i":42}""", encodeToString(subject))
        }
        parametrizedTest(Json(JsonConfiguration.Default.copy(encodeDefaults = false, alwaysDropNulls = true))) {
            assertEquals("""{"i":42}""", encodeToString(subject))
        }
    }

    @Test
    fun testDefaultsFlagIsStillWorking() {
        val subject = NullableData(42, null)
        parametrizedTest(Json(JsonConfiguration.Default.copy(encodeDefaults = true, alwaysDropNulls = false))) {
            assertEquals("""{"i":42,"s1":null,"s2":null}""", encodeToString(subject))
        }
        parametrizedTest(Json(JsonConfiguration.Default.copy(encodeDefaults = false, alwaysDropNulls = false))) {
            assertEquals("""{"i":42,"s1":null}""", encodeToString(subject))
        }
    }

    @Test
    fun testDropNullsFromList() {
        parametrizedTest(Json(JsonConfiguration(alwaysDropNulls = true))) {
            assertEquals("""{"l":["a","b","c"]}""", encodeToString(NullableInList(listOf("a", null, "b", null, "c"))))
            assertEquals("""{"l":["a","b","c"]}""", encodeToString(NullableInList(listOf(null, "a", "b", "c", null))))
            assertEquals("""{"l":[]}""", encodeToString(NullableInList(listOf(null, null))))
        }
    }

    @Test
    fun testDropNullsFromMap() {
        parametrizedTest(Json(JsonConfiguration(alwaysDropNulls = true))) {
            assertEquals(
                """{"m":{"1":"a","3":"b","5":"c"}}""",
                encodeToString(NullableInMap(mapOf(1 to "a", 2 to null, 3 to "b", 4 to null, 5 to "c")))
            )
            assertEquals(
                """{"m":{"2":"a","3":"b","4":"c"}}""",
                encodeToString(NullableInMap(mapOf(1 to null, 2 to "a", 3 to "b", 4 to "c", 5 to null)))
            )
            assertEquals("""{"m":{}}""", encodeToString(NullableInMap(mapOf(1 to null, 2 to null))))
        }
    }
}
