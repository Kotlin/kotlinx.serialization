package kotlinx.serialization.json.examples

import kotlinx.serialization.json.*

data class User(val id: Int, val name: String, val address: UserAddress?, val bag: UserProperties)

data class UserAddress(val country: String, val city: String?, val zipCode: Int?, val metadata: List<String>)

// Just bag of properties
data class UserProperties(val flag: Boolean, val int: Int, val double: Double, val nullableFlag: Boolean?)

object UserAddressParser : JsonParser<UserAddress>() {

    override fun write(value: UserAddress) = json {
        "country" to value.country
        "city" to value.city
        "zipCode" to value.zipCode
        "metadata" to JsonArray(value.metadata.map { JsonLiteral(it) })
    }

    override fun read(json: JsonObject): UserAddress {
        val country = json["country"].content
        val city = json["city"].contentOrNull
        val zipCode = json["zipCode"].intOrNull
        val metadata = json["metadata"].jsonArray.map { it.content }
        return UserAddress(country, city, zipCode, metadata)
    }
}

object UserPropertiesParser : JsonParser<UserProperties>() {

    override fun write(value: UserProperties) = json {
        "flag" to value.flag
        "int" to value.int
        "double" to value.double
        "nullableFlag" to value.nullableFlag
    }

    override fun read(json: JsonObject): UserProperties {
        val flag = json["flag"].boolean
        val int = json["int"].int
        val double = json["double"].double
        val nullableFlag = json["nullableFlag"].booleanOrNull
        return UserProperties(flag, int, double, nullableFlag)
    }
}

object UserParser : JsonParser<User>() {
    override fun write(value: User) = json {
        "id" to value.id
        "name" to value.name
        "address" to UserAddressParser.writeNullable(value.address)
        "bag" to UserPropertiesParser.write(value.bag)
    }

    override fun read(json: JsonObject): User {
        val id = json["id"].int
        val name = json["name"].content
        val address = UserAddressParser.read(json["address"])
        val bag = UserPropertiesParser.read(json["bag"].jsonObject)
        return User(id, name, address, bag)
    }
}
