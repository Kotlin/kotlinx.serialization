/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import org.junit.Ignore
import org.junit.Test
import kotlin.test.*

// can't be in common yet because of issue with class literal annotations
// and .serializer() resolving
class JsonUpdateCustomTest : JsonTestBase() {
    @Serializable
    data class Data(val a: Int)

    @Serializer(forClass = Data::class)
    object CustomDataUpdater {
        override fun patch(decoder: Decoder, old: Data): Data {
            val newData = decoder.decodeSerializableValue(this)
            return Data(old.a + newData.a)
        }
    }

    @Serializable
    data class Updatable(@Serializable(with = CustomDataUpdater::class) val d: Data)

    @Test
    fun canUpdateCustom() {
        val parsed: Updatable = unquotedLenient.parse("""{d:{a:42},d:{a:43}}""")
        assertEquals(Data(43), parsed.d)
    }

    @Serializable
    data class WrappedMap<T>(val mp: Map<String, T>)

    val json = Json(JsonConfiguration.Default)

    @Test
    fun canUpdateMap() {
        val parsed = json.parse(WrappedMap.serializer(Int.serializer()), """{"mp": { "x" : 23, "x" : 42, "y": 4 }}""")
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun canUpdateValuesInMap() {
        val parsed = json.parse(WrappedMap.serializer(Int.serializer().list), """{"mp": { "x" : [23], "x" : [42], "y": [4] }}""")
        assertEquals(WrappedMap(mapOf("x" to listOf(42), "y" to listOf(4))), parsed)
    }
}
