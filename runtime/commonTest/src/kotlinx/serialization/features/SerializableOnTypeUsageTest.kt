/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
        val str = Json.encodeToString(SerializableOnArguments.serializer(), data)
        assertEquals("""{"list1":[2,4],"list2":[[84]]}""", str)
        val restored = Json.decodeFromString(SerializableOnArguments.serializer(), str)
        assertEquals(data, restored)
    }
}
