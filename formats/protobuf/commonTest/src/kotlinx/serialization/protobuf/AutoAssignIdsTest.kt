/*
 * Copyright 2018 JetBrains s.r.o.
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

package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoAssignIdsTest {
    @Serializable
    data class WithoutIds(val a: Int, val b: String)

    @Serializable
    data class WithId(@SerialId(1) val a: Int, @SerialId(2) val b: String)

    @Test
    fun saveAndRestoreWithoutIds() {
        val w1 = WithoutIds(1, "foo")
        val bytes = ProtoBuf.dump(WithoutIds.serializer(), w1)
        val w2 = ProtoBuf.load(WithoutIds.serializer(), bytes)
        assertEquals(w1, w2)
    }

    @Test
    fun incrementalIds() {
        val w1 = WithoutIds(1, "foo")
        val bytes = ProtoBuf.dump(WithoutIds.serializer(), w1)
        val w2 = ProtoBuf.load(WithId.serializer(), bytes)
        assertEquals(w1.a, w2.a)
        assertEquals(w1.b, w2.b)
    }
}
