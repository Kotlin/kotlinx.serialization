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
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import io.kotlintest.specs.WordSpec
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.internal.HexConverter

class CBORWriterTest : WordSpec() {
    init {

        fun withEncoder(block: CBOR.CBOREncoder.() -> Unit): String {
            val result = ByteArrayOutputStream()
            CBOR.CBOREncoder(result).block()
            return HexConverter.printHexBinary(result.toByteArray()).toLowerCase()
        }

        // Examples from https://tools.ietf.org/html/rfc7049#appendix-A
        "CBOR Encoder" should {
            "encode integers" {
                val tabl = table(
                        headers("input", "output"),
                        row(0, "00"),
                        row(10, "0a"),
                        row(25, "1819"),
                        row(1000, "1903e8"),
                        row(-1, "20"),
                        row(-1000, "3903e7")
                )
                forAll(tabl) { input, output ->
                    withEncoder { encodeNumber(input.toLong()) } shouldBe output
                }
            }

            "encode doubles" {
                val tabl = table(
                        headers("input", "output"),
                        row(1.0e+300, "fb7e37e43c8800759c"),
                        row(-4.1, "fbc010666666666666")
                )
                forAll(tabl) { input, output ->
                    withEncoder { encodeDouble(input) } shouldBe output
                }
            }

            "encode strings" {
                val tabl = table(
                        headers("input", "output"),
                        row("IETF", "6449455446"),
                        row("\"\\", "62225c"),
                        row("\ud800\udd51", "64f0908591")
                )
                forAll(tabl) { input, output ->
                    withEncoder { encodeString(input) } shouldBe output
                }
            }
        }
    }
}