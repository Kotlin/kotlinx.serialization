/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UnknownElementIndexTest {
    enum class Choices { A, B, C }

    @Serializable
    data class Holder(val c: Choices)

    class MalformedReader : AbstractDecoder() {
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return UNKNOWN_NAME
        }
    }

    @Test
    fun testCompilerComplainsAboutIncorrectIndex() {
        assertFailsWith(UnknownFieldException::class) {
            MalformedReader().decodeSerializableValue(Holder.serializer())
        }
    }

    @Test
    fun testErrorMessage() {
        val message = "kotlinx.serialization.UnknownElementIndexTest.Choices does not contain element with name 'D'"
        assertFailsWith(SerializationException::class, message) {
            Json.decodeFromString(Holder.serializer(), """{"c":"D"}""")
        }
    }
}
