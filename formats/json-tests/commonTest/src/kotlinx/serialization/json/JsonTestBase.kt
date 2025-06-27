/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.efficientBinaryFormat.EfficientBinaryFormat
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.io.*
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.*
import kotlin.test.assertEquals
import okio.*
import kotlin.test.assertTrue
import kotlinx.io.Buffer as KotlinxIoBuffer
import okio.Buffer as OkioBuffer


enum class JsonTestingMode {
    STREAMING,
    TREE,
    OKIO_STREAMS,
    JAVA_STREAMS,
    KXIO_STREAMS,
    EFFICIENT_BINARY;

    companion object {
        fun value(i: Int) = values()[i]
    }
}

abstract class JsonTestBase {
    protected val default = Json { encodeDefaults = true }
    protected val lenient = Json { isLenient = true; ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true }

    internal inline fun <reified T : Any> Json.encodeToString(value: T, jsonTestingMode: JsonTestingMode): String {
        val serializer = serializersModule.serializer<T>()
        return encodeToString(serializer, value, jsonTestingMode)
    }

    @OptIn(ExperimentalStdlibApi::class)
    internal fun <T> Json.encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
        jsonTestingMode: JsonTestingMode
    ): String =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                encodeToString(serializer, value)
            }
            JsonTestingMode.JAVA_STREAMS -> {
                encodeViaStream(serializer, value)
            }
            JsonTestingMode.TREE -> {
                val tree = writeJson(this, value, serializer)
                encodeToString(tree)
            }
            JsonTestingMode.OKIO_STREAMS -> {
                val buffer = OkioBuffer()
                encodeToBufferedSink(serializer, value, buffer)
                buffer.readUtf8()
            }
            JsonTestingMode.KXIO_STREAMS -> {
                val buffer = KotlinxIoBuffer()
                encodeToSink(serializer, value, buffer)
                buffer.readString()
            }
            JsonTestingMode.EFFICIENT_BINARY -> {
                val ebf = EfficientBinaryFormat()
                val bytes = runCatching { ebf.encodeToByteArray(serializer, value) }.getOrElse { e->
                    null//throw e
                }
                if (bytes != null && serializer is KSerializer<*>) {
                    val decoded = ebf.decodeFromByteArray((serializer as KSerializer<T>), bytes)
                    encodeToString(serializer, decoded)
                } else {
                    encodeToString(serializer, value)
                }
            }
        }

    internal inline fun <reified T : Any> Json.decodeFromString(source: String, jsonTestingMode: JsonTestingMode): T {
        val deserializer = serializersModule.serializer<T>()
        return decodeFromString(deserializer, source, jsonTestingMode)
    }

    @OptIn(ExperimentalStdlibApi::class)
    internal fun <T> Json.decodeFromString(
        deserializer: DeserializationStrategy<T>,
        source: String,
        jsonTestingMode: JsonTestingMode
    ): T =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                decodeFromString(deserializer, source)
            }
            JsonTestingMode.JAVA_STREAMS -> {
                decodeViaStream(deserializer, source)
            }
            JsonTestingMode.TREE -> {
                val tree = decodeStringToJsonTree(this, deserializer, source)
                readJson(this, tree, deserializer)
            }
            JsonTestingMode.OKIO_STREAMS -> {
                val buffer = OkioBuffer()
                buffer.writeUtf8(source)
                decodeFromBufferedSource(deserializer, buffer)
            }
            JsonTestingMode.KXIO_STREAMS -> {
                val buffer = KotlinxIoBuffer()
                buffer.writeString(source)
                decodeFromSource(deserializer, buffer)
            }
            JsonTestingMode.EFFICIENT_BINARY -> {
                when (deserializer){
                    is KSerializer<*> -> {
                        val s = deserializer as KSerializer<T>
                        val value = decodeFromString(deserializer, source)
                        runCatching {
                            val ebf = EfficientBinaryFormat()
                            val binaryValue = EfficientBinaryFormat().encodeToByteArray(s, value)
                            ebf.decodeFromByteArray(s, binaryValue)
                        }.getOrElse { value }
                    }
                    else -> decodeFromString(deserializer, source)
                }
            }
        }

    protected open fun parametrizedTest(test: (JsonTestingMode) -> Unit) {
        processResults(buildList {
            add(runCatching { test(JsonTestingMode.STREAMING) })
            add(runCatching { test(JsonTestingMode.TREE) })
            add(runCatching { test(JsonTestingMode.OKIO_STREAMS) })
            add(runCatching { test(JsonTestingMode.KXIO_STREAMS) })
            add(runCatching { test(JsonTestingMode.EFFICIENT_BINARY) }
                .recover { e -> if ("Json format" in (e.message ?: "")) Unit else throw e })

            if (isJvm()) {
                add(runCatching { test(JsonTestingMode.JAVA_STREAMS) })
            }
        })
    }

    private inner class SwitchableJson(
        val json: Json,
        val jsonTestingMode: JsonTestingMode,
        override val serializersModule: SerializersModule = EmptySerializersModule()
    ) : StringFormat {
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            return json.encodeToString(serializer, value, jsonTestingMode)
        }

        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            return json.decodeFromString(deserializer, string, jsonTestingMode)
        }
    }

    /** A test runner that effectively handles the json tests to also test serialization to
     * "efficient" binary. This mainly checks serializer implementations.
     */
    private inner class EfficientBinary(
        val json: Json,
        val ebf: EfficientBinaryFormat = EfficientBinaryFormat(),
    ) : StringFormat {
        override val serializersModule: SerializersModule = ebf.serializersModule

        private var bytes: ByteArray? = null
        private var jsonStr: String? = null

        @OptIn(ExperimentalStdlibApi::class)
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            bytes = runCatching { ebf.encodeToByteArray(serializer, value) }
                .onFailure { if ("Json format" !in it.message!!) {
                    json.encodeToString(serializer, value) // trigger throwing the json exception if the exception is there
                    throw it
                } }
                .getOrNull()
            return json.encodeToString(serializer, value).also {
                if (bytes != null) jsonStr = it
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            /*
             * to retain compatibility with json we support different cases. If
             * the string has been encoded already use that. Instead, if the
             * deserializer is also a serializer (the default) then use that to
             * get the value from json and encode that to bytes which are then
             * decoded. In this case capture and ignore cases that require a
             * json encoder.
             *
             * Finally fall back to json decoding (nothing can be done)
             */

            var bytes = this@EfficientBinary.bytes
            if (string == jsonStr && bytes != null) {
                return ebf.decodeFromByteArray(deserializer, bytes)
            } else if (deserializer is SerializationStrategy<*>) {
                val value = json.decodeFromString(deserializer, string)
                //
                @Suppress("UNCHECKED_CAST")
                runCatching { ebf.encodeToByteArray(deserializer as SerializationStrategy<T>, value) }.onSuccess { r ->
                    bytes = r
                    jsonStr = string
                    return ebf.decodeFromByteArray(deserializer, bytes)
                }.onFailure { e ->
                    if ("Json format" !in e.message!!) throw e
                }
            }
            return json.decodeFromString(deserializer, string)
        }
    }

    protected fun parametrizedTest(json: Json, test: StringFormat.() -> Unit) {
        val streamingResult = runCatching { SwitchableJson(json, JsonTestingMode.STREAMING).test() }
        val treeResult = runCatching { SwitchableJson(json, JsonTestingMode.TREE).test() }
        val okioResult = runCatching { SwitchableJson(json, JsonTestingMode.OKIO_STREAMS).test() }
        val kxioResult = runCatching { SwitchableJson(json, JsonTestingMode.KXIO_STREAMS).test() }
        val efficientBinaryResult = runCatching { EfficientBinary(json).test() }
        processResults(listOf(streamingResult, treeResult, okioResult, kxioResult, efficientBinaryResult))
    }

    protected fun processResults(results: List<Result<*>>) {
        results.forEachIndexed { i, result ->
            result.onFailure {
                println("Failed test for ${JsonTestingMode.value(i)}")
                throw it
            }
        }
        for (i in results.indices) {
            for (j in results.indices) {
                if (i == j) continue
                assertEquals(
                    results[i].getOrNull()!!,
                    results[j].getOrNull()!!,
                    "Results differ for ${JsonTestingMode.value(i)} and ${JsonTestingMode.value(j)}"
                )
            }
        }
    }

    /**
     * Same as [assertStringFormAndRestored], but tests both json converters (streaming and tree)
     * via [parametrizedTest]
     */
    internal fun <T> assertJsonFormAndRestored(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        json: Json = default
    ) {
        parametrizedTest { jsonTestingMode ->
            val serialized = json.encodeToString(serializer, data, jsonTestingMode)
            assertEquals(expected, serialized, "Failed with streaming = $jsonTestingMode")
            val deserialized: T = json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertEquals(data, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
    /**
     * Same as [assertStringFormAndRestored], but tests both json converters (streaming and tree)
     * via [parametrizedTest]. Use custom checker for deserialized value.
     */
    internal fun <T> assertJsonFormAndRestoredCustom(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        check: (T, T) -> Boolean
    ) {
        parametrizedTest { jsonTestingMode ->
            val serialized = Json.encodeToString(serializer, data, jsonTestingMode)
            assertEquals(expected, serialized, "Failed with streaming = $jsonTestingMode")
            val deserialized: T = Json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertTrue("Failed with streaming = $jsonTestingMode\n\tsource value =$data\n\tdeserialized value=$deserialized") { check(data, deserialized) }
        }
    }

    internal fun <T> assertRestoredFromJsonForm(
        serializer: KSerializer<T>,
        jsonForm: String,
        expected: T,
    ) {
        parametrizedTest { jsonTestingMode ->
            val deserialized: T = Json.decodeFromString(serializer, jsonForm, jsonTestingMode)
            assertEquals(expected, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
}
