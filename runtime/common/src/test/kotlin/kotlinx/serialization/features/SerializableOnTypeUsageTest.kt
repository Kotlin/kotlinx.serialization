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

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals


@Serializable
data class SerializableOnArguments(
    val list1: List<@Serializable(MultiplyingIntSerializer::class) Int>,
    val list2: List<List<@Serializable(MultiplyingIntHolderSerializer::class) IntHolder>>
)

class SerializableOnTypeUsageTest {
    @Test
    fun testAnnotationIsApplied() {
        val data = SerializableOnArguments(listOf(1, 2), listOf(listOf(IntHolder(42))))
        val str = Json.stringify(SerializableOnArguments.serializer(), data)
        assertEquals("""{"list1":[2,4],"list2":[[84]]}""", str)
        val restored = Json.parse(SerializableOnArguments.serializer(), str)
        assertEquals(data, restored)
    }
}
