/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringData
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertFailsWithMessage
import org.junit.Ignore
import org.junit.Test
import java.io.*
import kotlin.test.assertEquals

class JsonStreamFlowTest {
    val json = Json {}

    suspend inline fun <reified T> Flow<T>.writeToStream(os: OutputStream) {
        collect {
            json.encodeToStream(it, os)
        }
    }

    val inputString = """{"data":"a"}{"data":"b"}{"data":"c"}"""
    val inputList = listOf(StringData("a"), StringData("b"), StringData("c"))

    @Test
    fun testEncodeSeveralItems() {
        val items = inputList
        val os = ByteArrayOutputStream()
        runBlocking {
            val f = flow<StringData> { items.forEach { emit(it) } }
            f.writeToStream(os)
        }

        assertEquals(inputString, os.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun testDecodeSeveralItems() {
        val ins = ByteArrayInputStream(inputString.encodeToByteArray())
        assertFailsWithMessage<SerializationException>("EOF") {
            json.decodeFromStream<StringData>(ins)
        }
    }


}
