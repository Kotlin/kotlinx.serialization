/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// TODO: Move to common tests after https://youtrack.jetbrains.com/issue/KT-28927 is fixed

@file:UseContextualSerialization(Int::class, IntHolder::class)

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*
import kotlinx.serialization.modules.*

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
    val json = Json { serializersModule = module }

    @Test
    fun testOnFile() {
        val str = json.encodeToString(Carrier3.serializer(), Carrier3(IntHolder(42), 8, 8, IntHolder(42)))
        assertEquals(
            """{"a":84,"i":4,"nullable":4,"nullableIntHolder":84,"nullableIntList":[],"nullableIntHolderNullableList":null}""",
            str
        )
    }
}
