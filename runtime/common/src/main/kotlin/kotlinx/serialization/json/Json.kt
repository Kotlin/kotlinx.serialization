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
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.context.SerialModule
import kotlinx.serialization.json.internal.*
import kotlin.jvm.*

class Json(
    @JvmField internal val unquoted: Boolean = false,
    @JvmField internal val indented: Boolean = false,
    @JvmField internal val indent: String = "    ",
    @JvmField internal val strictMode: Boolean = true,
    val updateMode: UpdateMode = UpdateMode.OVERWRITE,
    val encodeDefaults: Boolean = true
): AbstractSerialFormat(), StringFormat {

    override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String {
        val result = StringBuilder()
        val output = JsonOutput(
            result, this,
            WriteMode.OBJ,
            arrayOfNulls(WriteMode.values().size)
        )
        output.encode(serializer, obj)
        return result.toString()
    }

    override fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T {
        val parser = JsonParser(string)
        val input = JsonInput(this, WriteMode.OBJ, parser)
        val result = input.decode(serializer)
        check(parser.tokenClass == TC_EOF) { "Shall parse complete string"}
        return result
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

        override fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T =
            plain.parse(serializer, string)
    }
}
