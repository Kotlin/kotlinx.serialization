package kotlinx.serialization.json

import kotlinx.serialization.KSerializer

class JsonTreeImplicitNullsTest: AbstractJsonImplicitNullsTest() {
    override fun <T> Json.encode(value: T, serializer: KSerializer<T>): String {
        return encodeToJsonElement(serializer, value).toString()
    }

    override fun <T> Json.decode(json: String, serializer: KSerializer<T>): T {
        val jsonElement = parseToJsonElement(json)
        return decodeFromJsonElement(serializer, jsonElement)
    }
}
