import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.ValueTransformer
import utils.shop

/**
 * This demo shows how one can easily apply transformations
 * (which are very similar to functional lens by their nature) to arbitrary objects
 * on arbitrary nesting levels
 *
 * @author Roman Elizarov
 */

object LowercaseTransformer : ValueTransformer() {
    override fun transformStringValue(desc: KSerialClassDesc, index: Int, value: String): String =
            value.toLowerCase()
}

fun main(args: Array<String>) {
    val p = shop
    println("Original shop: $p")
    val q = LowercaseTransformer.transform(p)
    println("Transformed shop: $q")
}