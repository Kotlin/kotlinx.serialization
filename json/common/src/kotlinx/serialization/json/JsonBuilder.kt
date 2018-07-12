@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.json

/**
 * Builds [JsonObject] with given [init] builder
 */
fun json(init: JsonBuilder.() -> Unit): JsonObject {
    val builder = JsonBuilder()
    builder.init()
    return JsonObject(builder.content)
}

class JsonBuilder(internal val content: MutableMap<String, JsonElement> = mutableMapOf()) {

    /**
     * Adds given value to outer [JsonObject] with [this] as a key
     */
    public infix fun String.to(value: JsonElement) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = value
    }

    /**
     * Adds given value to outer [JsonObject] with [this] as a key as [JsonPrimitive]
     */
    public infix fun String.to(value: Number?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given value to outer [JsonObject] with [this] as a key as [JsonPrimitive]
     */
    public infix fun String.to(value: Boolean?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given value to outer [JsonObject] with [this] as a key as [JsonPrimitive]
     */
    public infix fun String.to(value: String?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }
}
