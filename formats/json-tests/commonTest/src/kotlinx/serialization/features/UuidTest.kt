/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*
import kotlin.uuid.*

class UuidTest : JsonTestBase() {
    @Test
    fun testPlainUuid() {
        val uuid = Uuid.random()
        assertJsonFormAndRestored(Uuid.serializer(), uuid, "\"$uuid\"")
    }

    // TODO: write a test without @Contextual after 2.1.0 release
    @Serializable
    data class Holder(@Contextual val uuid: Uuid)

    val json = Json { serializersModule = serializersModuleOf(Uuid.serializer()) }

    @Test
    fun testNested() {
        val fixed = Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1")
        assertJsonFormAndRestored(
            Holder.serializer(),
            Holder(fixed),
            """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""",
            json
        )
    }
}
