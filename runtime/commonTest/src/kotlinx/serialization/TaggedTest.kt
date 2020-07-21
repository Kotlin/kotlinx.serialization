/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*
import kotlinx.serialization.internal.*

class TaggedTest {

    @Serializable
    data class DataWithId(
        @Id(1) val first: Int,
        @Id(2) val second: String,
        val noId: Unit = Unit,
        @Id(42) val last: Boolean = true
    )

    class Collector : TaggedEncoder<Int?>() {
        override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)
        val tagList = mutableMapOf<Int?, Any>()
        override fun encodeTaggedValue(tag: Int?, value: Any) {
            tagList[tag] = value
        }
    }

    class Emitter(private val collected: Collector) : TaggedDecoder<Int?>() {
        private var i = 0

        override fun decodeSequentially(): Boolean = true
        override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            // js doesn't generate code for .decodeSequentially for the sake of keeping output small
            if (!isJs()) throw AssertionError("Should not be called in this test due to support of decodeSequentially")
            return if (i == collected.tagList.size) CompositeDecoder.DECODE_DONE else i++
        }

        override fun decodeTaggedValue(tag: Int?): Any {
            return collected.tagList.getValue(tag)
        }
    }

    @Test
    @Ignore
    fun testTagged() {
        val collector = Collector()
        val data = DataWithId(1, "2")
        collector.encodeSerializableValue(DataWithId.serializer(), data)
        assertEquals(mapOf(1 to 1, 2 to "2", null to Unit, 42 to true), collector.tagList, "see all tags properly")
        val obj = Emitter(collector).decodeSerializableValue(DataWithId.serializer())
        assertEquals(obj, data, "read tags back")
    }
}
