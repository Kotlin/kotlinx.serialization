package kotlinx.serialization.json.parser.examples

import kotlinx.serialization.json.*

abstract class JsonParser<T: Any> {

    abstract fun write(value: T): JsonElement

    fun writeNullable(value: T?): JsonElement {
        if (value == null) return JsonNull
        return write(value)
    }

    abstract fun read(json: JsonObject): T

    fun read(json: JsonElement): T? {
        if (json.isNull) return null
        return read(json.jsonObject)
    }

    fun read(string: String): T = read(Json.parseJson(string).jsonObject)
}
