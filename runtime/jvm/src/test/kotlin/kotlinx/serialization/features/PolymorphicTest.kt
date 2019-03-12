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

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.*
import org.junit.Test
import java.text.*
import java.util.*
import kotlin.test.*

@Serializable
open class A(@SerialId(1) val id: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as A

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "A(id=$id)"
    }

}

@Serializable
data class B(@SerialId(2) val s: String) : A(1)


class PolymorphicTest {

    @Serializable
    data class Wrapper(@SerialId(1) val a1: A, @SerialId(2) val a2: A)

    @Serializable
    data class DateWrapper(@SerialId(1) @Serializable(with = PolymorphicSerializer::class) val date: Date)

    @Serializer(forClass = Date::class)
    object DateSerializer {
        private val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS")

        override fun serialize(encoder: Encoder, obj: Date) {
            encoder.encodeString(df.format(obj))
        }

        override fun deserialize(decoder: Decoder): Date {
            return df.parse(decoder.decodeString())
        }
    }

    @Test
    fun testInheritanceJson() {
        val obj = Wrapper(A(2), B("b"))
        val bytes = Json.unquoted.stringify(obj)
        assertEquals("{a1:[kotlinx.serialization.features.A,{id:2}]," +
                "a2:[kotlinx.serialization.features.B,{id:1,s:b}]}", bytes)
    }

    @Test
    fun testInheritanceProtobuf() {
        val obj = Wrapper(A(2), B("b"))
        val bytes = ProtoBuf.dumps(obj)
        val restored = ProtoBuf.loads<Wrapper>(bytes)
        assertEquals(obj, restored)
    }

    @Test
    fun testPolymorphicWrappedOverride() {
        kotlinx.serialization.registerSerializer("java.util.Date", DateSerializer)
        val obj = DateWrapper(Date())
        val bytes = ProtoBuf.dumps(obj)
        val restored = ProtoBuf.loads<DateWrapper>(bytes)
        assertEquals(obj, restored)
    }

    @Test
    fun testExplicit() {
        val obj = B("b")
        val s = Json.unquoted.stringify(PolymorphicSerializer, obj)
        assertEquals("[kotlinx.serialization.features.B,{id:1,s:b}]", s)
    }
}
