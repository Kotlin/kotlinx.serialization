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

package kotlinx.serialization

import kotlinx.serialization.json.Json
import org.junit.Test
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.Map
import kotlin.collections.hashMapOf
import kotlin.collections.hashSetOf
import kotlin.test.assertEquals


class JavaCollectionsTest {
    @Serializable
    data class HasHashMap(
        val s: String,
        val hashMap: HashMap<Int, String>,
        val hashSet: HashSet<Int>,
        val linkedHashMap: LinkedHashMap<Int, String>,
        val kEntry: Map.Entry<Int, String>?
    )

    @Test
    fun test() {
        val original = HasHashMap("42", hashMapOf(1 to "1", 2 to "2"), hashSetOf(11), LinkedHashMap(), null)
        val serializer = HasHashMap.serializer()
        val string = Json.stringify(serializer = serializer, value = original)
        assertEquals(
            expected = """{"s":"42","hashMap":{"1":"1","2":"2"},"hashSet":[11],"linkedHashMap":{},"kEntry":null}""",
            actual = string
        )
        val restored = Json.parse(deserializer = serializer, string = string)
        assertEquals(expected = original, actual = restored)
    }
}
