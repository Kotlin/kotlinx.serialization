package kotlinx.serialization.json.parser.examples

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
        val country = json.getValue("country").content
        val city = json["city"]?.contentOrNull
        val zipCode = json["zipCode"]?.intOrNull
        val metadata = json.getValue("metadata").jsonArray.map { it.content }
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
        val flag = json.getValue("flag").boolean
        val int = json.getValue("int").int
        val double = json.getValue("double").double
        val nullableFlag = json["nullableFlag"]?.booleanOrNull
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
        val id = json.getValue("id").int
        val name = json.getValue("name").content
        val address = UserAddressParser.read(json.getValue("address"))
        val bag = UserPropertiesParser.read(json.getValue("bag").jsonObject)
        return User(id, name, address, bag)
    }
}
