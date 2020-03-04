package utils

import kotlinx.serialization.Serializable

// simple data objects

@Serializable
data class CityData(
        val id: Int,
        val name: String
)

@Serializable
data class StreetData(
        val id: Int,
        val name: String,
        val city: CityData
)

@Serializable
data class StreetData2(
        val id: Int,
        val name: String,
        val city: CityData?
)

@Serializable
data class CountyData(
        val name: String,
        val cities: List<CityData>
)

// Shop from Kotlin Koans

@Serializable
data class Shop(val name: String, val customers: List<Customer>)

@Serializable
data class Customer(val name: String, val city: City, val orders: List<Order>) {
    override fun toString() = "$name from ${city.name} with $orders"
}

@Serializable
data class Order(val products: List<Product>, val isDelivered: Boolean) {
    override fun toString() = "$products${ if (isDelivered) " delivered" else "" }"
}

@Serializable
data class Product(val name: String, val price: Double) {
    override fun toString() = "'$name' for $price"
}

@Serializable
data class City(val name: String) {
    override fun toString() = name
}

// TestShop from Kotlin Koans

//products
val idea = Product("IntelliJ IDEA Ultimate", 199.0)
val reSharper = Product("ReSharper", 149.0)
val dotTrace = Product("DotTrace", 159.0)
val dotMemory = Product("DotTrace", 129.0)
val dotCover = Product("DotCover", 99.0)
val appCode = Product("AppCode", 99.0)
val phpStorm = Product("PhpStorm", 99.0)
val pyCharm = Product("PyCharm", 99.0)
val rubyMine = Product("RubyMine", 99.0)
val webStorm = Product("WebStorm", 49.0)
val teamCity = Product("TeamCity", 299.0)
val youTrack = Product("YouTrack", 500.0)

//customers
val lucas = "Lucas"
val cooper = "Cooper"
val nathan = "Nathan"
val reka = "Reka"
val bajram = "Bajram"
val asuka = "Asuka"

//cities
val Canberra = City("Canberra")
val Vancouver = City("Vancouver")
val Budapest = City("Budapest")
val Ankara = City("Ankara")
val Tokyo = City("Tokyo")

fun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())
fun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)
fun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())

val shop = shop("jb test shop",
        customer(lucas, Canberra,
                order(reSharper),
                order(reSharper, dotMemory, dotTrace)
        ),
        customer(cooper, Canberra),
        customer(nathan, Vancouver,
                order(rubyMine, webStorm)
        ),
        customer(reka, Budapest,
                order(idea, isDelivered = false),
                order(idea, isDelivered = false),
                order(idea)
        ),
        customer(bajram, Ankara,
                order(reSharper)
        ),
        customer(asuka, Tokyo,
                order(idea)
        )
)

// Zoo from library tests by Roman Elizarov

enum class Attitude { POSITIVE, NEUTRAL, NEGATIVE }

@Serializable
data class IntData(val intV: Int)

@Serializable
data class Tree(val name: String, val left: Tree? = null, val right: Tree? = null)

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
        val enum: Attitude,
        val intData: IntData,
        val unitN: Unit?,
        val booleanN: Boolean?,
        val byteN: Byte?,
        val shortN: Short?,
        val intN: Int?,
        val longN: Long?,
        val floatN: Float?,
        val doubleN: Double?,
        val charN: Char?,
        val stringN: String?,
        val enumN: Attitude?,
        val intDataN: IntData?,
        val listInt: List<Int>,
        val listIntN: List<Int?>,
        val listNInt: List<Int>?,
        val listNIntN: List<Int?>?,
        val listListEnumN: List<List<Attitude?>>,
        val listIntData: List<IntData>,
        val listIntDataN: List<IntData?>,
        val tree: Tree,
        val mapStringInt: Map<String,Int>,
        val mapIntStringN: Map<Int,String?>,
        val arrays: ZooWithArrays
)

@Serializable data class ZooWithArrays(
        val arrByte: Array<Byte>,
        val arrInt: Array<Int>,
        val arrIntN: Array<Int?>,
        val arrIntData: Array<IntData>

) {
    override fun equals(other: Any?) = other is ZooWithArrays &&
            arrByte.contentEquals(other.arrByte) &&
            arrInt.contentEquals(other.arrInt) &&
            arrIntN.contentEquals(other.arrIntN) &&
            arrIntData.contentEquals(other.arrIntData)
}

val zoo = Zoo(
        Unit, true, 10, 20, 30, 40, 50f, 60.0, 'A', "Str0", Attitude.POSITIVE, IntData(70),
        null, null, 11, 21, 31, 41, 51f, 61.0, 'B', "Str1", Attitude.NEUTRAL, null,
        listOf(1, 2, 3),
        listOf(4, 5, null),
        listOf(6, 7, 8),
        listOf(null, 9, 10),
        listOf(listOf(Attitude.NEGATIVE, null)),
        listOf(IntData(1), IntData(2), IntData(3)),
        listOf(IntData(1), null, IntData(3)),
        Tree("root", Tree("left"), Tree("right", Tree("right.left"), Tree("right.right"))),
        mapOf("one" to 1, "two" to 2, "three" to 3),
        mapOf(0 to null, 1 to "first", 2 to "second"),
        ZooWithArrays(
                arrayOf(1, 2, 3),
                arrayOf(100, 200, 300),
                arrayOf(null, -1, -2),
                arrayOf(IntData(1), IntData(2))
        )
)
