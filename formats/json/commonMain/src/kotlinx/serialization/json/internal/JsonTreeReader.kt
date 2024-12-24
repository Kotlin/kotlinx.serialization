/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
internal class JsonTreeReader(
    configuration: JsonConfiguration,
    private val lexer: AbstractJsonLexer
) {
    private val isLenient = configuration.isLenient
    private val trailingCommaAllowed = configuration.allowTrailingComma
    private var stackDepth = 0

    private fun readObject(): JsonElement = readObjectImpl {
        read()
    }

    private suspend fun DeepRecursiveScope<Unit, JsonElement>.readObject(): JsonElement =
        readObjectImpl { callRecursive(Unit) }

    private inline fun readObjectImpl(reader: () -> JsonElement): JsonObject {
        lexer.consumeNextToken(TC_BEGIN_OBJ)
        lexer.path.pushDescriptor(JsonObjectSerializer.descriptor)

        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = linkedMapOf<String, JsonElement>()
        while (lexer.canConsumeValue()) {
            lexer.path.resetCurrentMapKey()
            // Read key and value
            val key = if (isLenient) lexer.consumeStringLenient() else lexer.consumeString()
            lexer.consumeNextToken(TC_COLON)

            lexer.path.updateCurrentMapKey(key)
            val element = reader()
            result[key] = element
            // Verify the next token
            lexer.consumeCommaOrPeekEnd(
                allowTrailing = trailingCommaAllowed,
                expectedEnd = '}',
            )
        }

        lexer.consumeNextToken(TC_END_OBJ)
        lexer.path.popDescriptor()

        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        lexer.consumeNextToken(TC_BEGIN_LIST)
        lexer.path.pushDescriptor(JsonArraySerializer.descriptor)
        // Prohibit leading comma
        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = arrayListOf<JsonElement>()
        while (lexer.canConsumeValue()) {
            lexer.path.updateDescriptorIndex(result.size)
            val element = read()
            result.add(element)
            lexer.consumeCommaOrPeekEnd(
                allowTrailing = trailingCommaAllowed,
                expectedEnd = ']',
                entity = "array"
            )
        }
        lexer.consumeNextToken(TC_END_LIST)
        lexer.path.popDescriptor()

        return JsonArray(result)
    }

    private fun readValue(isString: Boolean): JsonPrimitive {
        val string = if (isLenient || !isString) {
            lexer.consumeStringLenient()
        } else {
            lexer.consumeString()
        }
        if (!isString && string == NULL) return JsonNull
        return JsonLiteral(string, isString)
    }

    fun read(): JsonElement {
        return when (val token = lexer.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> {
                /*
                 * If the object has the depth of 200 (an arbitrary "good enough" constant), it means
                 * that it's time to switch to stackless recursion to avoid StackOverflowError.
                 * This case is quite rare and specific, so more complex nestings (e.g. through
                 * the chain of JsonArray and JsonElement) are not supported.
                 */
                val result = if (++stackDepth == 200) {
                    readDeepRecursive()
                } else {
                    readObject()
                }
                --stackDepth
                result
            }

            TC_BEGIN_LIST -> readArray()
            else -> lexer.fail("Cannot read Json element because of unexpected ${tokenDescription(token)}")
        }
    }

    private fun readDeepRecursive(): JsonElement = DeepRecursiveFunction<Unit, JsonElement> {
        when (lexer.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> lexer.fail("Can't begin reading element, unexpected token")
        }
    }.invoke(Unit)
}
