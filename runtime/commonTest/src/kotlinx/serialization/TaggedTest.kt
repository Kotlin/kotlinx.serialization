/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.test.isJs
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
        private var i = 0
        override fun decodeSequentially(): Boolean = true

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            // js doesn't generate code for .decodeSequentially for the sake of keeping output small
            if (!isJs()) throw AssertionError("Should not be called in this test due to support of decodeSequentially")
            return if (i == collected.tagList.size) READ_DONE else i++
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
        val obj = Emitter(collector)
            .decode(DataWithId.serializer())
        assertEquals(obj, data, "read tags back")
    }

    @Test
    fun testMapper() {
        val data = DataWithId(1, "2")
        Mapper.map(DataWithId.serializer(), data) shouldBe mapOf("first" to 1, "second" to "2", "noId" to Unit, "last" to true)
    }

}

