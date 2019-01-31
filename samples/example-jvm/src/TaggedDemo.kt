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

    class Collector : TaggedEncoder<Int>() {

        val tagList = mutableMapOf<Int, Any>()

        override fun SerialDescriptor.getTag(index: Int): Int {
            return getElementAnnotations(index).filterIsInstance<MyId>().single().id
        }

        override fun encodeTaggedValue(tag: Int, value: Any) {
            tagList[tag] = value
        }
    }

    class Emitter(val collected: Collector) : TaggedDecoder<Int>() {

        override fun SerialDescriptor.getTag(index: Int): Int {
            return getElementAnnotations(index).filterIsInstance<MyId>().single().id
        }

        override fun decodeTaggedValue(tag: Int): Any {
            return collected.tagList.getValue(tag)
        }
    }
}

fun testMyIdIo(serializer: KSerializer<Any>, obj: Any): Result {
    val collector = MyIdMapper.Collector()
    collector.encode(serializer, obj)

    val emitter = MyIdMapper.Emitter(collector)
    val other = emitter.decode(serializer)
    return Result(obj, other, "Tag map: ${collector.tagList}")
}

fun main(args: Array<String>) {
    testCase(DataWithMyId.serializer(), DataWithMyId(1, "42"), ::testMyIdIo)
}
