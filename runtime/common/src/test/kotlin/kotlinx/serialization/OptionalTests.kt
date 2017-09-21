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

import kotlinx.serialization.json.JSON
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OptionalTests {

    @Serializable
    class Data(val a: Int = 0, @Optional val b: Int = 42) {
        @Optional
        var c = "Hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false

            return true
        }

    }

    @Test
    fun testAll() {
        assertEquals("{a:0,b:42,c:Hello}", JSON.unquoted.stringify(Data()))
        assertEquals(JSON.unquoted.parse<Data>("{a:0,b:43,c:Hello}"), Data(b = 43))
        assertEquals(JSON.unquoted.parse<Data>("{a:0,b:42,c:Hello}"), Data())
    }

    @Test
    fun testMissingOptionals() {
        assertEquals(JSON.unquoted.parse<Data>("{a:0,c:Hello}"), Data())
        assertEquals(JSON.unquoted.parse<Data>("{a:0}"), Data())
    }

    @Test
    fun testThrowMissingField() {
        assertFailsWith(MissingFieldException::class) {
            JSON.unquoted.parse<Data>("{b:0}")
        }
    }

}
