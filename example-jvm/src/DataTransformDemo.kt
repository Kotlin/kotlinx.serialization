import kotlinx.serialization.*
import utils.Shop
import utils.shop

/**
 * This demo shows how one can easily apply transformations
 * (which are very similar to functional lens by their nature) to arbitrary objects
 * on arbitrary nesting levels
 */

object LowercaseTransformer : ValueTransformer() {
    override fun transformStringValue(desc: SerialDescriptor, index: Int, value: String): String =
            value.toLowerCase()
}

fun main(args: Array<String>) {
    val p = shop
    println("Original shop: $p")
    val q = LowercaseTransformer.transform(Shop.serializer(), p)
    println("Transformed shop: $q")
}
