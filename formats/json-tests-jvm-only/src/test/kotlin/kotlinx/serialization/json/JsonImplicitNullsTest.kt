package kotlinx.serialization.json

import kotlinx.serialization.*

class JsonImplicitNullsTest: AbstractJsonImplicitNullsTest() {
    override fun <T> Json.encode(value: T, serializer: KSerializer<T>): String {
        return encodeToString(serializer, value)
    }

    override fun <T> Json.decode(json: String, serializer: KSerializer<T>): T {
        return decodeFromString(serializer, json)
    }
}
