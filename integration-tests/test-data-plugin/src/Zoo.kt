// Auto-generated file, do not modify!
import kotlin.Boolean
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlinx.serialization.Serializable

enum class Attitude {
    POSITIVE,

    NEUTRAL,

    NEGATIVE
}

@Serializable
data class Simple(val a: String)

@Serializable
data class SmallZoo(
        val str: String,
        val i: Int,
        val nullable: Double?,
        val list: List<Int>,
        val map: Map<Int, Boolean>,
        val inner: SmallZoo,
        val innerList: List<Simple>
)

@Serializable
data class Zoo(
        val unit: Unit,
        val boolean: Boolean,
        val byte: Byte,
        val short: Short,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val char: Char,
        val string: String,
        val simple: Simple,
        val enum: Attitude,
        val booleanN: Boolean?,
        val byteN: Byte?,
        val shortN: Short?,
        val intN: Int?,
        val longN: Long?,
        val floatN: Float?,
        val doubleN: Double?,
        val charN: Char?,
        val stringN: String?,
        val simpleN: Simple?,
        val enumN: Attitude?,
        val listInt: List<Int>,
        val listIntN: List<Int?>,
        val setNInt: Set<Int>,
        val mutableSetNIntN: Set<Int?>,
        val listListSimple: List<List<Simple>>,
        val listListSimpleN: List<List<Simple?>>,
        val map: Map<String, Int>,
        val mapN: Map<Int, String?>
)
