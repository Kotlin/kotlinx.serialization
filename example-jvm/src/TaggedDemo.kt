import kotlinx.serialization.*
import utils.Result
import utils.testCase

/**
 * This demo shows usage of user-defined serial annotations and Tagged inputs/outputs.
 *
 * Denotable function is KSerialClassDesc.getTag, yet its signature and usage is obvious.
 *
 * Also you must explicitly specify annotation target as property
 * on @SerialInfo annotation classes
 */

@Target(AnnotationTarget.PROPERTY)
@SerialInfo
annotation class MyId(val id: Int)

@Serializable
data class DataWithMyId(@MyId(1) val a: Int, @MyId(2) val b: String)

object MyIdMapper {

    class Collector : TaggedOutput<Int>() {

        val tagList = mutableMapOf<Int, Any>()

        override fun KSerialClassDesc.getTag(index: Int): Int {
            return getAnnotationsForIndex(index).filterIsInstance<MyId>().single().id
        }

        override fun writeTaggedValue(tag: Int, value: Any) {
            tagList[tag] = value
        }
    }

    class Emitter(val collected: Collector) : TaggedInput<Int>() {

        override fun KSerialClassDesc.getTag(index: Int): Int {
            return getAnnotationsForIndex(index).filterIsInstance<MyId>().single().id
        }

        override fun readTaggedValue(tag: Int): Any {
            return collected.tagList.getValue(tag)
        }
    }
}

fun testMyIdIo(serializer: KSerializer<Any>, obj: Any): Result {
    val collector = MyIdMapper.Collector()
    collector.write(serializer, obj)

    val emitter = MyIdMapper.Emitter(collector)
    val other = emitter.read(serializer)
    return Result(obj, other, "Tag map: ${collector.tagList}")
}

fun main(args: Array<String>) {
    testCase(DataWithMyId::class.serializer(), DataWithMyId(1, "42"), ::testMyIdIo)
}