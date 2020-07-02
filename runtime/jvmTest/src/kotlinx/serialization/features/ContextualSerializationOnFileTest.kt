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

// TODO: Move to common tests after https://youtrack.jetbrains.com/issue/KT-28927 is fixed

@file:ContextualSerialization(Int::class, IntHolder::class)

package kotlinx.serialization.features

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class Carrier3(
    val a: IntHolder,
    val i: Int,
    val nullable: Int?,
    val nullableIntHolder: IntHolder?,
    val nullableIntList: List<Int?> = emptyList(),
    val nullableIntHolderNullableList: List<IntHolder?>? = null
)

class ContextualSerializationOnFileTest {
    val module = SerializersModule {
        contextual(DividingIntSerializer)
        contextual(MultiplyingIntHolderSerializer)
    }
    val json = Json(context = module)

    @Test
    fun testOnFile() {
        val str = json.encodeToString(Carrier3.serializer(), Carrier3(IntHolder(42), 8, 8, IntHolder(42)))
        assertEquals(
            """{"a":84,"i":4,"nullable":4,"nullableIntHolder":84,"nullableIntList":[],"nullableIntHolderNullableList":null}""",
            str
        )
    }
}
