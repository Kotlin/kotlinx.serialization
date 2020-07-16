/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import io.kotlintest.matchers.*
import io.kotlintest.properties.*
import io.kotlintest.specs.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*

class CborWriterSpecTest : WordSpec() {
    init {

        fun withEncoder(block: CborEncoder.() -> Unit): String {
            val result = ByteArrayOutput()
            CborEncoder(result).block()
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
