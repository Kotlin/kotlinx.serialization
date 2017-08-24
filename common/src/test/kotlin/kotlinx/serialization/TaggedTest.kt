package kotlinx.serialization

import org.junit.Test
import kotlin.test.assertEquals

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

class TaggedTest {

    @Serializable
    data class DataWithId(
            @SerialId(1) val first: Int,
            @SerialId(2) val second: String,
            val noId: Unit = Unit,
            @Optional @SerialId(42) val last: Boolean = true
    )

    class Collector : IntTaggedOutput() {
        val tagList = mutableMapOf<Int?, Any>()
        override fun writeTaggedValue(tag: Int?, value: Any) {
            tagList[tag] = value
        }
    }

    class Emitter(val collected: Collector) : IntTaggedInput() {
        override fun readTaggedValue(tag: Int?): Any {
            return collected.tagList.getValue(tag)
        }
    }

    infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)

    @Test
    fun testTagged() {
        val collector = Collector()
        val data = DataWithId(1, "2")
        collector.write(DataWithId.Companion, data)

        assertEquals(mapOf(1 to 1, 2 to "2", null to Unit, 42 to true), collector.tagList, "see all tags properly")
        val obj = Emitter(collector).read(DataWithId::class.serializer())
        assertEquals(obj, data, "read tags back")
    }

    @Test
    fun testMapper() {
        val data = DataWithId(1, "2")
        Mapper.map(data) shouldBe mapOf("first" to 1, "second" to "2", "noId" to Unit, "last" to true)
    }

}

