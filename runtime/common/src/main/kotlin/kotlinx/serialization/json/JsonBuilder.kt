/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Builds [JsonArray] with given [init] builder
 */
fun jsonArray(init: JsonArrayBuilder.() -> Unit): JsonArray {
    val builder = JsonArrayBuilder()
    builder.init()
    return JsonArray(builder.content)
}

class JsonArrayBuilder(internal val content: MutableList<JsonElement> = mutableListOf()) {
    /**
     * Adds [this] value to outer [JsonArray] as [JsonPrimitive]
     */
    public operator fun String?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to outer [JsonArray] as [JsonPrimitive]
     */
    public operator fun Number?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to outer [JsonArray] as [JsonPrimitive]
     */
    public operator fun Boolean?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to outer [JsonArray]
     */
    public operator fun JsonElement.unaryPlus() {
        this@JsonArrayBuilder.content.add(this)
    }
}

class JsonBuilder(internal val content: MutableMap<String, JsonElement> = linkedMapOf()) {

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
