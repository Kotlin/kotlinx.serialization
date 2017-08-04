/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlinx.serialization.formats.cbor

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.WordSpec
import kotlinx.serialization.CBOR
import kotlinx.serialization.internal.HexConverter
import java.io.ByteArrayInputStream

/**
 * @author Leonid Startsev
 *		  sandwwraith@gmail.com
 **/

class CBORReaderTest : WordSpec() {
    init {

        fun withDecoder(input: String, block: CBOR.CBORDecoder.() -> Unit) {
            val bytes = HexConverter.parseHexBinary(input.toUpperCase())
            CBOR.CBORDecoder(ByteArrayInputStream(bytes)).block()
        }

        "CBOR Decoder" should {
            "decode integers" {
                withDecoder("0C1903E8", {
                    nextNumber() shouldBe 12L
                    nextNumber() shouldBe 1000L
                })
                withDecoder("203903e7", {
                    nextNumber() shouldBe -1L
                    nextNumber() shouldBe -1000L
                })
            }

            "decode strings"{
                withDecoder("6568656C6C6F", {
                    nextString() shouldBe "hello"
                })
                withDecoder("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273", {
                    nextString() shouldBe "string that is longer than 23 characters"
                })
            }

            "decode floats and doubles" {
                withDecoder("fb7e37e43c8800759c", {
                    nextDouble() shouldBe 1e+300
                })
                withDecoder("fa47c35000", {
                    nextFloat() shouldBe 100000.0f
                })
            }
        }

        "CBOR Reader" should {
            "read simple object" {
                CBOR.loads<Simple>("bf616163737472ff") shouldBe Simple("str")
            }

            "read complicated object" {
                val test = SmallZoo(
                        "Hello, world!",
                        42,
                        null,
                        listOf("a", "b"),
                        mapOf(1 to true, 2 to false),
                        Simple("lol"),
                        listOf(Simple("kek"))
                )

                CBOR.loads<SmallZoo>(
                        "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
                ) shouldBe test
            }
        }
    }
}