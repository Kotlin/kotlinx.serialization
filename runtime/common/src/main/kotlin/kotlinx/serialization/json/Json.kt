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
package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.context.*
import kotlinx.serialization.json.internal.*
import kotlin.jvm.*

public class Json(
    @JvmField internal val unquoted: Boolean = false,
    @JvmField internal val indented: Boolean = false,
    @JvmField internal val indent: String = "    ",
    @JvmField internal val strictMode: Boolean = true,
    val updateMode: UpdateMode = UpdateMode.OVERWRITE,
    val encodeDefaults: Boolean = true
): AbstractSerialFormat(), StringFormat {

    public override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String {
        val result = StringBuilder()
        val encoder = StreamingJsonOutput(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        encoder.encode(serializer, obj)
        return result.toString()
    }

    public override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T {
        val parser = JsonReader(string)
        val input = StreamingJsonInput(this, WriteMode.OBJ, parser)
        val result = input.decode(deserializer)
        if (!parser.isDone) {
            error("Parser has not consumed the whole input")
        }
        return result
    }

    public fun <T> fromJson(json: JsonElement, deserializer: DeserializationStrategy<T>): T {
        return readJson(json, deserializer)
    }

    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> fromJson(tree: JsonElement): T = fromJson(tree, context.getOrDefault(T::class))

    public fun <T> toJson(value: T, serializer: SerializationStrategy<T>): JsonElement {
        return writeJson(value, serializer)
    }

    companion object : StringFormat {
        val plain = Json()
        val unquoted = Json(unquoted = true)
        val indented = Json(indented = true)
        val nonstrict = Json(strictMode = false)

        override fun install(module: SerialModule) = plain.install(module)
        override val context: SerialContext get() = plain.context
        override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String =
            plain.stringify(serializer, obj)

        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
            plain.parse(deserializer, string)
    }
}
