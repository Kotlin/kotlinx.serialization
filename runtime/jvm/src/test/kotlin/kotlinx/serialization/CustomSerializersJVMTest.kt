/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.context.*
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CustomSerializersJVMTest {

    @Serializable
    data class Data(val a: Int, @Optional val b: Int = 42)

    @Serializable
    data class EnhancedData(
            val data: Data,
            val stringPayload: Payload,
            @Serializable(with = BinaryPayloadSerializer::class) val binaryPayload: Payload
    )

    data class Payload(val s: String)

    @Serializable
    data class PayloadList(val ps: List<Payload>)

    @Serializer(forClass = Payload::class)
    object PayloadSerializer {}

    object BinaryPayloadSerializer : KSerializer<Payload> {
        override val descriptor: SerialDescriptor = SerialClassDescImpl("Payload")

        override fun serialize(output: Encoder, obj: Payload) {
            output.encodeString(HexConverter.printHexBinary(obj.s.toByteArray()))
        }

        override fun deserialize(input: Decoder): Payload {
            return Payload(String(HexConverter.parseHexBinary(input.decodeString())))
        }
    }

    private val obj = EnhancedData(Data(100500), Payload("string"), Payload("binary"))
    private lateinit var json: JSON

    @Before
    fun initContext() {
        val scope = SimpleModule(Payload::class, PayloadSerializer)
        json = JSON(unquoted = true).apply { install(scope) }
    }

    @Test
    fun writeCustom() {
        val s = json.stringify(obj)
        assertEquals("{data:{a:100500,b:42},stringPayload:{s:string},binaryPayload:62696E617279}", s)
    }

    @Test
    fun readCustom() {
        val s = json.parse<EnhancedData>("{data:{a:100500,b:42},stringPayload:{s:string},binaryPayload:62696E617279}")
        assertEquals(obj, s)
    }

    @Test
    fun writeCustomList() {
        val s = json.stringify(PayloadList(listOf(Payload("1"), Payload("2"))))
        assertEquals("{ps:[{s:1},{s:2}]}", s)
    }

    @Test
    fun testPolymorphicResolve() {
        val map = mapOf<String, Any>("Payload" to Payload("data"))
        val saver = (StringSerializer to PolymorphicSerializer).map
        val s = json.stringify(saver, map)
        assertEquals("""{Payload:[kotlinx.serialization.CustomSerializersJVMTest.Payload,{s:data}]}""", s)
    }
}
