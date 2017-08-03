package kotlinx.tagged

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.ShouldSpec
import kotlinx.serialization.*

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

class TaggedTest : ShouldSpec() {

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

    class Emitter(val collected: Collector): IntTaggedInput() {
        override fun readTaggedValue(tag: Int?): Any {
            return collected.tagList.getValue(tag)
        }
    }

    init {
        val collector = Collector()
        val data = DataWithId(1, "2")
        collector.write(data)
        should("see all tags properly") {
            collector.tagList shouldBe mapOf(1 to 1, 2 to "2", null to Unit, 42 to true)
        }
        should("read tags back") {
            val obj = Emitter(collector).read(DataWithId::class.serializer())
            obj shouldBe data
        }
        should("map field names to values") {
            Mapper.map(data) shouldBe mapOf("first" to 1, "second" to "2", "noId" to Unit, "last" to true)
        }
    }

}

