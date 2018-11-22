/*
 * Copyright 2017 JetBrains s.r.o.
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

import kotlinx.serialization.json.*
import kotlin.test.*

class TransientTests {
    @Serializable
    class Data(val a: Int = 0, @Transient var b: Int = 42, @Optional val e: Boolean = false) {
        @Optional
        var c = "Hello"

        @Transient
        val d: String
            get() = "hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false
            if (d != other.d) return false

            return true
        }

        override fun toString(): String {
            return "Data(a=$a, b=$b, e=$e, c='$c', d='$d')"
        }
    }

    @Test
    fun testAll() {
        assertEquals("{a:0,e:false,c:Hello}", Json.unquoted.stringify(Data()))
    }

    @Test
    fun testMissingOptionals() {
        assertEquals(Json.unquoted.parse("{a:0,c:Hello}"), Data())
        assertEquals(Json.unquoted.parse("{a:0}"), Data())
    }

    @Test
    fun testThrowTransient() {
        assertFailsWith(SerializationException::class) {
            Json.unquoted.parse<Data>("{a:0,b:100500,c:Hello}")
        }
    }

}
