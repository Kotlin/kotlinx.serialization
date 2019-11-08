/*
 * Copyright 2018 JetBrains s.r.o.
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

import kotlinx.serialization.test.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

class TaggedTest {

    @Serializable
    data class DataWithId(
            @SerialId(1) val first: Int,
            @SerialId(2) val second: String,
            val noId: Unit = Unit,
            @SerialId(42) val last: Boolean = true
    )

    class Collector : IntTaggedEncoder() {
        val tagList = mutableMapOf<Int?, Any>()
        override fun encodeTaggedValue(tag: Int?, value: Any) {
            tagList[tag] = value
        }
    }

    class Emitter(val collected: Collector) : IntTaggedDecoder() {
        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            TODO("Should not be called")
        }

        override fun decodeTaggedValue(tag: Int?): Any {
            return collected.tagList.getValue(tag)
        }
    }

    @Test
    fun testTagged() {
        val collector = Collector()
        val data = DataWithId(1, "2")
        collector.encode(DataWithId.serializer(), data)

        assertEquals(mapOf(1 to 1, 2 to "2", null to Unit, 42 to true), collector.tagList, "see all tags properly")
        val obj = Emitter(collector).decode(DataWithId.serializer())
        assertEquals(obj, data, "read tags back")
    }

    @Test
    fun testMapper() {
        val data = DataWithId(1, "2")
        Mapper.map(data) shouldBe mapOf("first" to 1, "second" to "2", "noId" to Unit, "last" to true)
    }

}

