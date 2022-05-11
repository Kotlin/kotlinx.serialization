/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.features.sealed.SealedChild
import kotlinx.serialization.features.sealed.SealedParent
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.JsonDecodingException
import kotlinx.serialization.test.assertFailsWithMessage
import org.junit.Test
import java.io.*
import kotlin.test.*

class JsonLazySequenceTest {
    val json = Json

    private suspend inline fun <reified T> Flow<T>.writeToStream(os: OutputStream) {
        collect {
            json.encodeToStream(it, os)
        }
    }

    private suspend inline fun <reified T> Json.readFromStream(iss: InputStream): Flow<T> = flow {
        val serial = serializer<T>()
        val iter = iterateOverStream(iss, serial)
        while (iter.hasNext()) {
            emit(iter.next())
        }
    }.flowOn(Dispatchers.IO)

    private val inputStringWsSeparated = """{"data":"a"}{"data":"b"}{"data":"c"}"""
    private val inputStringWrapped = """[{"data":"a"},{"data":"b"},{"data":"c"}]"""
    private val inputList = listOf(StringData("a"), StringData("b"), StringData("c"))

    @Test
    fun testEncodeSeveralItems() {
        val items = inputList
        val os = ByteArrayOutputStream()
        runBlocking {
            val f = flow<StringData> { items.forEach { emit(it) } }
            f.writeToStream(os)
        }

        assertEquals(inputStringWsSeparated, os.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun testDecodeSeveralItems() {
        val ins = ByteArrayInputStream(inputStringWsSeparated.encodeToByteArray())
        assertFailsWithMessage<SerializationException>("EOF") {
            json.decodeFromStream<StringData>(ins)
        }
    }

    private inline fun <reified T> Iterator<T>.assertNext(expected: T) {
        assertTrue(hasNext())
        assertEquals(expected, next())
    }

    private fun <T> Json.iterateOverStream(stream: InputStream, deserializer: DeserializationStrategy<T>): Iterator<T> =
        decodeToSequence(stream, deserializer).iterator()

    private fun withInputs(vararg inputs: String = arrayOf(inputStringWsSeparated, inputStringWrapped), block: (InputStream) -> Unit) {
        for (input in inputs) {
            val res = runCatching { block(input.asInputStream()) }
            if (res.isFailure) throw AssertionError("Failed test with input $input", res.exceptionOrNull())
        }
    }

    private fun String.asInputStream() = ByteArrayInputStream(this.encodeToByteArray())

    @Test
    fun testIterateSeveralItems() = withInputs { ins ->
        val iter = json.iterateOverStream(ins, StringData.serializer())
        iter.assertNext(StringData("a"))
        iter.assertNext(StringData("b"))
        iter.assertNext(StringData("c"))
        assertFalse(iter.hasNext())
        assertFailsWithMessage<SerializationException>("EOF") {
            iter.next()
        }
    }

    @Test
    fun testDecodeToSequence() = withInputs { ins ->
        val sequence = json.decodeToSequence(ins, StringData.serializer())
        assertEquals(inputList, sequence.toList(), "For input $inputStringWsSeparated")
        assertFailsWith<IllegalStateException> { sequence.toList() } // assert constrained once
    }

    @Test
    fun testDecodeAsFlow() = withInputs { ins ->
        val list = runBlocking {
            buildList { json.readFromStream<StringData>(ins).toCollection(this) }
        }
        assertEquals(inputList, list)
    }

    @Test
    fun testItemsSeparatedByWs() {
        val input = "{\"data\":\"a\"}   {\"data\":\"b\"}\n\t{\"data\":\"c\"}"
        val ins = ByteArrayInputStream(input.encodeToByteArray())
        assertEquals(inputList, json.decodeToSequence(ins, StringData.serializer()).toList())
    }

    @Test
    fun testJsonElement() {
        val list = listOf<JsonElement>(
            buildJsonObject { put("foo", "bar") },
            buildJsonObject { put("foo", "baz") },
            JsonPrimitive(10),
            JsonPrimitive("abacaba"),
            buildJsonObject { put("foo", "qux") }
        )
        val inputWs = """${list[0]} ${list[1]} ${list[2]}    ${list[3]}    ${list[4]}"""
        val decodedWs = json.decodeToSequence<JsonElement>(inputWs.asInputStream()).toList()
        assertEquals(list, decodedWs, "Failed whitespace-separated test")
        val inputArray = """[${list[0]}, ${list[1]},${list[2]}  ,  ${list[3]}    ,${list[4]}]"""
        val decodedArrayWrap = json.decodeToSequence<JsonElement>(inputArray.asInputStream()).toList()
        assertEquals(list, decodedArrayWrap, "Failed array-wrapped test")
    }


    @Test
    fun testSealedClasses() {
        val input = """{"type":"first child","i":1,"j":10} {"type":"first child","i":1,"j":11}"""
        val iter = json.iterateOverStream(input.asInputStream(), SealedParent.serializer())
        iter.assertNext(SealedChild(10))
        iter.assertNext(SealedChild(11))
    }

    @Test
    fun testMalformedArray() {
        val input1 = """[1, 2, 3"""
        val input2 = """[1, 2, 3]qwert"""
        val input3 = """[1,2 3]"""
        withInputs(input1, input2, input3) {
            assertFailsWith<JsonDecodingException> {
                json.decodeToSequence(it, Int.serializer()).toList()
            }
        }
    }

    @Test
    fun testMultilineArrays() {
        val input = "[1,2,3]\n[4,5,6]\n[7,8,9]"
        assertFailsWith<JsonDecodingException> {
            json.decodeToSequence<List<Int>>(input.asInputStream(), DecodeSequenceMode.AUTO_DETECT).toList()
        }
        assertFailsWith<JsonDecodingException> {
            json.decodeToSequence<Int>(input.asInputStream(), DecodeSequenceMode.AUTO_DETECT).toList()
        }
        assertFailsWith<JsonDecodingException> { // we do not merge lists
            json.decodeToSequence<Int>(input.asInputStream(), DecodeSequenceMode.ARRAY_WRAPPED).toList()
        }
        val parsed = json.decodeToSequence<List<Int>>(input.asInputStream(), DecodeSequenceMode.WHITESPACE_SEPARATED).toList()
        val expected = listOf(listOf(1,2,3), listOf(4,5,6), listOf(7,8,9))
        assertEquals(expected, parsed)
    }

    @Test
    fun testStrictArrayCheck() {
        assertFailsWith<JsonDecodingException> {
            json.decodeToSequence<StringData>(inputStringWsSeparated.asInputStream(), DecodeSequenceMode.ARRAY_WRAPPED)
        }
    }

    @Test
    fun testPaddedWs() {
        val paddedWs = "  $inputStringWsSeparated  "
        assertEquals(inputList, json.decodeToSequence(paddedWs.asInputStream(), StringData.serializer()).toList())
        assertEquals(inputList, json.decodeToSequence(paddedWs.asInputStream(), StringData.serializer(), DecodeSequenceMode.WHITESPACE_SEPARATED).toList())
    }

    @Test
    fun testPaddedArray() {
        val paddedWs = "  $inputStringWrapped  "
        assertEquals(inputList, json.decodeToSequence(paddedWs.asInputStream(), StringData.serializer()).toList())
        assertEquals(inputList, json.decodeToSequence(paddedWs.asInputStream(), StringData.serializer(), DecodeSequenceMode.ARRAY_WRAPPED).toList())
    }

}
