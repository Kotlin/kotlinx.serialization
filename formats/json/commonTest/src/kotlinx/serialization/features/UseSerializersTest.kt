/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:UseSerializers(MultiplyingIntHolderSerializer::class, MultiplyingIntSerializer::class)

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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
    @Test
    fun testOnFile() {
        val str = Json.encodeToString(Carrier2.serializer(), Carrier2(IntHolder(42), 2, 2, IntHolder(42)))
        assertEquals("""{"a":84,"i":4,"nullable":4,"nullableIntHolder":84,"nullableIntList":[],"nullableIntHolderNullableList":null}""", str)
    }
}
