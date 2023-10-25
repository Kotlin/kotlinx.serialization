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
import kotlinx.serialization.test.typeTokenOf
import org.junit.Test
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.Map
import kotlin.collections.hashMapOf
import kotlin.collections.hashSetOf
import kotlin.reflect.*
import kotlin.test.*


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
    fun testJavaCollectionsInsideClass() {
        val original = HasHashMap("42", hashMapOf(1 to "1", 2 to "2"), hashSetOf(11), LinkedHashMap(), null)
        val serializer = HasHashMap.serializer()
        val string = Json.encodeToString(serializer = serializer, value = original)
        assertEquals(
            expected = """{"s":"42","hashMap":{"1":"1","2":"2"},"hashSet":[11],"linkedHashMap":{},"kEntry":null}""",
            actual = string
        )
        val restored = Json.decodeFromString(deserializer = serializer, string = string)
        assertEquals(expected = original, actual = restored)
    }

    @Test
    fun testTopLevelMaps() {
        // Returning null here is a deliberate choice: map constructor functions may return different specialized
        // implementations (e.g., kotlin.collections.EmptyMap or java.util.Collections.SingletonMap)
        // that may or may not be generic. Since we generally cannot return a generic serializer using Java class only,
        // all attempts to get map serializer using only .javaClass should return null.
        assertNull(serializerOrNull(emptyMap<String, String>().javaClass))
        assertNull(serializerOrNull(mapOf<String, String>("a" to "b").javaClass))
        assertNull(serializerOrNull(mapOf<String, String>("a" to "b", "b" to "c").javaClass))
        // Correct ways of retrieving map serializer:
        assertContains(
            serializer(typeTokenOf<Map<String, String>>()).descriptor.serialName,
            "kotlin.collections.LinkedHashMap"
        )
        assertContains(
            serializer(typeTokenOf<java.util.LinkedHashMap<String, String>>()).descriptor.serialName,
            "kotlin.collections.LinkedHashMap"
        )
        assertContains(
            serializer(typeOf<LinkedHashMap<String, String>>()).descriptor.serialName,
            "kotlin.collections.LinkedHashMap"
        )
    }

    @Test
    fun testTopLevelSetsAndLists() {
        // Same reasoning as for maps
        assertNull(serializerOrNull(emptyList<String>().javaClass))
        assertNull(serializerOrNull(listOf<String>("a").javaClass))
        assertNull(serializerOrNull(listOf<String>("a", "b").javaClass))
        assertNull(serializerOrNull(emptySet<String>().javaClass))
        assertNull(serializerOrNull(setOf<String>("a").javaClass))
        assertNull(serializerOrNull(setOf<String>("a", "b").javaClass))
        assertContains(
            serializer(typeTokenOf<Set<String>>()).descriptor.serialName,
            "kotlin.collections.LinkedHashSet"
        )
        assertContains(
            serializer(typeTokenOf<List<String>>()).descriptor.serialName,
            "kotlin.collections.ArrayList"
        )
        assertContains(
            serializer(typeTokenOf<java.util.LinkedHashSet<String>>()).descriptor.serialName,
            "kotlin.collections.LinkedHashSet"
        )
        assertContains(
            serializer(typeTokenOf<java.util.ArrayList<String>>()).descriptor.serialName,
            "kotlin.collections.ArrayList"
        )
    }

    @Test
    fun testAnonymousObject() {
        val obj: Any = object {}
        assertNull(serializerOrNull(obj.javaClass))
    }
}

