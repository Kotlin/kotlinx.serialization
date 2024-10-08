/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:UseSerializers(MultiplyingIntHolderSerializer::class, MultiplyingIntSerializer::class)

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

@Serializable
data class Carrier2(
    val a: IntHolder,
    val i: Int,
    val nullable: Int?,
    val nullableIntHolder: IntHolder?,
    val nullableIntList: List<Int?> = emptyList(),
    val nullableIntHolderNullableList: List<IntHolder?>? = null
)

class UseSerializersTest {
    private val jsonWithDefaults = Json { encodeDefaults = true }

    @Test
    fun testOnFile() {
        val str = jsonWithDefaults.encodeToString(
            Carrier2.serializer(),
            Carrier2(IntHolder(42), 2, 2, IntHolder(42))
        )
        assertEquals("""{"a":84,"i":4,"nullable":4,"nullableIntHolder":84,"nullableIntList":[],"nullableIntHolderNullableList":null}""", str)
    }
}
