import kotlinx.serialization.KSerializer
import kotlinx.serialization.ValueTransformer
import utils.Result
import utils.testMethod

/**
 * This demo shows that you can easily clone your object
 * by using transformer class without defined transformations
 */

object Clone : ValueTransformer() {}

fun testCloneIO(serializer: KSerializer<Any>, obj: Any): Result {
    // clone
    val other = Clone.transform(serializer, obj)
    // result
    return Result(obj, other, "cloned")
}

fun main(args: Array<String>) {
    testMethod(::testCloneIO)
}