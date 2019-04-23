/*
 * Copyright 2019 JetBrains s.r.o.
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

package kotlinx.io

import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.test.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteBufferTest {
    @Test
    fun longsAndDoubles() {
        ByteBuffer.allocate(8).putDouble(199.0).print() shouldBe "4068E00000000000"
        ByteBuffer.allocate(8).putLong(Int.MAX_VALUE.toLong() + 2).print() shouldBe "0000000080000001"
        ByteBuffer.from("4068E00000000000").getDouble() shouldBe 199.0
        ByteBuffer.from("0000000080000001").getLong() shouldBe Int.MAX_VALUE.toLong() + 2
    }

    @Test
    fun byteOrder() {
        val bb = ByteBuffer.allocate(4)

        // reading test
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.put(0)
        bb.put(0)
        bb.put(5)
        bb.put(57)
        bb.flip()
        assertEquals(1337, bb.getInt())
        bb.flip()
        bb.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(956628992, bb.getInt())
        bb.flip()
        bb.order(ByteOrder.BIG_ENDIAN)
        assertEquals(1337, bb.getInt())

        // writing test
        bb.clear()
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.putInt(1337)
        bb.flip()
        assertEquals(0, bb.get())
        assertEquals(0, bb.get())
        assertEquals(5, bb.get())
        assertEquals(57, bb.get())

        bb.clear()
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(1337)
        bb.flip()
        assertEquals(57, bb.get())
        assertEquals(5, bb.get())
        assertEquals(0, bb.get())
        assertEquals(0, bb.get())
    }

    private fun ByteBuffer.print() = HexConverter.printHexBinary(this.array())

    @Suppress("NAME_SHADOWING")
    private fun ByteBuffer.Companion.from(source: String) = HexConverter.parseHexBinary(source).let { source ->
        ByteBuffer.allocate(source.size).put(source).flip()
    }
}
