/*
 * Copyright 2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {

    private val originalData = Data("Hello", mapOf(42 to "forty-two"))
    private val originalString = """{"s":"Hello","m":{"42":"forty-two"}}"""

    @Test
    fun testStringForm() {
        val str = Json.stringify(Data.serializer(), originalData)
        assertEquals(originalString, str)
    }

    @Test
    fun testSerializeBack() {
        val restored = Json.parse(Data.serializer(), originalString)
        assertEquals(originalData, restored)
    }
}
