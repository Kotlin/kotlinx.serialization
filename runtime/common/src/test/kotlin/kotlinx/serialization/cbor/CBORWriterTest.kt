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


package kotlinx.serialization.cbor

import kotlin.test.Test
import kotlin.test.assertEquals


class CBORWriterTest {
    infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)

    @Test
    fun writeSimpleClass() {
        CBOR.dumps(Simple("str")) shouldBe "bf616163737472ff"
    }

    @Test
    fun writeComplicatedClass() {
        val test = SmallZoo(
                "Hello, world!",
                42,
                null,
                listOf("a", "b"),
                mapOf(1 to true, 2 to false),
                Simple("lol"),
                listOf(Simple("kek"))
        )
        CBOR.dumps(test) shouldBe "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
    }

    @Test
    fun writeManyNumbers() {
        val test = NumberZoo(
                100500,
                Long.MAX_VALUE,
                42.0f,
                1235621356215.0,
                true,
                'a'
        )
        CBOR.dumps(test) shouldBe "bf63696e741a00018894646c6f6e671b7fffffffffffffff65666c6f6174fa4228000066646f75626c65fb4271fb0c5a2b700067626f6f6c65616ef564636861721861ff"
    }
}
