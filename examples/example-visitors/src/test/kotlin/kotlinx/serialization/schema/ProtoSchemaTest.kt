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

package kotlinx.serialization.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumberType
import kotlinx.serialization.protobuf.ProtoType
import kotlinx.serialization.schema.*
import kotlin.test.*


@Serializable
private data class Simple(val int: Int, @SerialId(10) val embedded: SimpleEmbedded, @SerialId(25) val someMap: Map<String, String> = emptyMap())

@Serializable
private data class SimpleEmbedded(val int0: Int, @ProtoType(ProtoNumberType.SIGNED) val int1: Int)

class ProtoSchemaTest {

    private val correctSchemaText = """message ${Simple.serializer().descriptor.name} {
  required int32 int = 1;
  required ${SimpleEmbedded.serializer().descriptor.name} embedded = 10;
  map<string, string> someMap = 25;
}"""

    @Test
    fun test() {
        val schema = ProtoSchema(Simple.serializer().descriptor)
        assertEquals(correctSchemaText, schema.toString())
        with(schema) {
            assertEquals(3, fields.size)
            assertEquals("int", fields[0].name)
            assertEquals(VarintType.int32, fields[0].type)
            assertTrue(fields[1].type is ProtoMessage)
            assertEquals(VarintType.sint32, (fields[1].type as ProtoMessage).fields[1].type)
        }
    }
}
