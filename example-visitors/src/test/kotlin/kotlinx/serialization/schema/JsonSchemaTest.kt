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

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlin.test.*

@Serializable
private data class Data2(@Optional val l: List<Int> = emptyList(), val s: String)

class JsonSchemaTest {
    @Test
    fun jsonSchema() {
        val desc: SerialDescriptor = Data2.serializer().descriptor
        val schema = JsonSchema(desc)

        val correctSchema = json {
            "description" to desc.name
            "type" to "object"
            "required" to jsonArray { +"s" }
            "properties" to json {
                "l" to json {
                    "description" to ArrayListClassDesc(IntDescriptor).name
                    "type" to "array"
                    "items" to json { "type" to "number"; "description" to IntDescriptor.name }
                }
                "s" to json {
                    "description" to StringDescriptor.name
                    "type" to "string"
                }
            }
        }

        assertEquals(correctSchema, schema)
    }
}
