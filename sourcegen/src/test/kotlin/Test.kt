import junit.framework.Assert.assertEquals
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test

/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

class Test {
    @Test
    fun testWrite() {
        val d = MyData(10, choice = Choice.RIGHT)
        val j = JSON.unquoted.stringify(MyData.serializer, d)
        assertEquals("{x:10,y:foo,intList:[1,2,3],choice:RIGHT}", j)
    }

    @Test
    fun testRead() {
        val d = MyData(10, intList = listOf(1, 2))
        val j = JSON.unquoted.parse(MyData.serializer, "{x:10,y:foo,intList:[1,2],choice:LEFT}")
        assertEquals(d, j)
        val j2 = JSON.unquoted.parse(MyData.serializer, "{x:null,intList:[1,2,3],choice:LEFT}")
        assertEquals(MyData(null), j2)
    }

    @Test
    fun testProto() {
        val d = MyData(10, intList = listOf(1, 2))
        val bytes = ProtoBuf.dump(MyData.serializer, d)
        println("Proto diagnostics: ${HexConverter.printHexBinary(bytes)}")
        val d2 = ProtoBuf.load(MyData.serializer, bytes)
        assertEquals(d, d2)

    }
}
