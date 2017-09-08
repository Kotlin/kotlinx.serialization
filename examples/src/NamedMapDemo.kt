import kotlinx.serialization.KSerializer
import kotlinx.serialization.NamedValueInput
import kotlinx.serialization.NamedValueOutput
import utils.*

/**
 * This demo shows another approach to serialization:
 * Instead of writing fields and their values separately in two steps, as ElementValueOutput does,
 * in NamedValueOutput they got merged into one call.
 *
 * NamedValue is a subclass of TaggedValue, which allows you to associate any custom tag with object's field,
 * see TaggedDemo.kt.
 *
 * Here, the tag is field's name. Functionality of these classes is similar to kotlinx.serialization.Mapper
 * Note that null values are not supported here.
 *
 * @author Roman Elizarov
 */

class MapOutput(val map: MutableMap<String, Any> = mutableMapOf()) : NamedValueOutput() {
    override fun writeTaggedValue(name: String, value: Any) {
        map[name] = value
    }
}

class MapInput(val map: Map<String, Any>) : NamedValueInput() {
    override fun readTaggedValue(name: String): Any {
        return map[name]!!
    }
}

fun testMapIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val out = MapOutput()
    out.write(serializer, obj)
    // load
    val inp = MapInput(out.map)
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${out.map.size} items ${out.map}")
}

fun main(args: Array<String>) {
    testCase(Shop, shop, ::testMapIO)
}