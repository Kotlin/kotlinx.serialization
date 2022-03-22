/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.features.sealed.SealedChild
import kotlinx.serialization.features.sealed.SealedParent
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.JsonDecodingException
import kotlinx.serialization.test.assertFailsWithMessage
import org.junit.Test
import java.io.*
import kotlin.test.*

class JsonSequencePathTest {

    @Serializable
    class NestedData(val s: String)

    @Serializable
    class Data(val data: NestedData)

    @Test
    fun testFailure() {
        val source = """{"data":{"s":"value"}}{"data":{"s":42}}{notevenreached}""".toStream()
        val iterator = Json.decodeToSequence<Data>(source).iterator()
        iterator.next() // Ignore
        assertFailsWithMessage<SerializationException>(
            "Expected quotation mark '\"', but had '2' instead at path: \$.data.s"
        ) { iterator.next() }
    }

    private fun String.toStream() = ByteArrayInputStream(encodeToByteArray())
}
