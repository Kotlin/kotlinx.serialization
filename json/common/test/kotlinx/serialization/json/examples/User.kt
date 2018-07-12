package kotlinx.serialization.json.examples

import kotlinx.serialization.json.*

data class User(val id: Int, val name: String, val address: UserAddress?, val bag: UserProperties)

data class UserAddress(val country: String, val city: String?, val zipCode: Int?, val metadata: List<String>)

// Just bag of properties
data class UserProperties(val flag: Boolean, val int: Int, val double: Double, val nullableFlag: Boolean?)

object UserAddressParser : JsonParser<UserAddress>(UserAddress::class) {

    override fun write(value: UserAddress): JsonElement {
        val country = JsonLiteral(value.country)
        val city = value.city?.let { JsonLiteral(it) } ?: JsonNull
        val zipCode = value.zipCode?.let { JsonLiteral(it) } ?: JsonNull
        val metadata = JsonArray(value.metadata.map { JsonLiteral(it) })
        return JsonObject(
            mapOf(
                "country" to country,
                "city" to city,
                "zipCode" to zipCode,
                "metadata" to metadata
            )
        )
    }

    override fun read(json: JsonObject): UserAddress {
        val country = json["country"].primitive.content
        val city = with(json["city"]) {
            val js = JsonNull
            val test = js === JsonNull
            if (isNull) null
            else primitive.content
        }
        val zipCode = json["zipCode"].primitive.asIntOrNull
        val metadata = json["metadata"].jsonArray.map { it.primitive.content }
        return UserAddress(country, city, zipCode, metadata)
    }
}

object UserPropertiesParser : JsonParser<UserProperties>(UserProperties::class) {

    override fun write(value: UserProperties): JsonElement {
        return with(value) {
            JsonObject(mapOf(
                "flag" to JsonLiteral(flag),
                "int" to JsonLiteral(int),
                "double" to JsonLiteral(double),
                "nullableFlag" to (nullableFlag?.let { JsonLiteral(nullableFlag) } ?: JsonNull)))
        }
    }

    override fun read(json: JsonObject): UserProperties {
        val flag = json["flag"].primitive.asBoolean
        val int = json["int"].primitive.asInt
        val double = json["double"].primitive.asDouble
        val nullableFlag = json["nullableFlag"].primitive.asBooleanOrNull
        return UserProperties(flag, int, double, nullableFlag)
    }
}

object UserParser : JsonParser<User>(User::class) {
    override fun write(value: User): JsonElement {
        return with(value) {
            JsonObject(mapOf(
                "id" to JsonLiteral(id),
                "name" to JsonLiteral(name),
                "address" to (address?.let { UserAddressParser.write(it) } ?: JsonNull),
                "bag" to UserPropertiesParser.write(bag)))
        }
    }

    override fun read(json: JsonObject): User {
        val id = json["id"].primitive.asInt
        val name = json["name"].primitive.content // TODO replace content globally
        val address = with(json["address"]) {
            if (isNull) null
            else UserAddressParser.read(jsonObject) }
        val bag = UserPropertiesParser.read(json["bag"].jsonObject)
        return User(id, name, address, bag)
    }
}
