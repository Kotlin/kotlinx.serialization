/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.cbor.*

@Serializable
data class SimpleData(val foo: String, val bar: Int)

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
data class CountryData(
    val name: String,
    val cities: List<CityData> = emptyList()
)

val russia = CountryData("Russia", listOf(
    CityData(1, "Saint-Petersburg"),
    CityData(2, "Moscow"),
    CityData(3, "Yekaterinburg")
))

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
