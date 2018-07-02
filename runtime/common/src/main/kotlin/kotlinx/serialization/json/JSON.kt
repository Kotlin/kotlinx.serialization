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
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME

data class JSON(
        private val unquoted: Boolean = false,
        private val indented: Boolean = false,
        private val indent: String = "    ",
        internal val nonstrict: Boolean = false,
        val updateMode: UpdateMode = UpdateMode.OVERWRITE,
        val context: SerialContext? = null
) {
    fun <T> stringify(saver: SerializationStrategy<T>, obj: T): String {
        val sb = StringBuilder()
        val output = JsonOutput(Mode.OBJ, Composer(sb), arrayOfNulls(Mode.values().size))
        output.encode(saver, obj)
        return sb.toString()
    }

    inline fun <reified T : Any> stringify(obj: T): String = stringify(context.klassSerializer(T::class), obj)

    fun <T> parse(loader: DeserializationStrategy<T>, str: String): T {
        val parser = Parser(str)
        val input = JsonInput(Mode.OBJ, parser)
        val result = input.decode(loader)
        check(parser.tc == TC_EOF) { "Shall parse complete string"}
        return result
    }

    inline fun <reified T : Any> parse(str: String): T = parse(context.klassSerializer(T::class), str)

    companion object {
        fun <T> stringify(saver: SerializationStrategy<T>, obj: T): String = plain.stringify(saver, obj)
        inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)
        fun <T> parse(loader: DeserializationStrategy<T>, str: String): T = plain.parse(loader, str)
        inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)

        val plain = JSON()
        val unquoted = JSON(unquoted = true)
        val indented = JSON(indented = true)
        val nonstrict = JSON(nonstrict = true)
    }

    inner class JsonOutput internal constructor(private val mode: Mode, private val w: Composer, private val modeReuseCache: Array<JsonOutput?>) : ElementValueEncoder() {
        init {
            context = this@JSON.context
            val i = mode.ordinal
            if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
                modeReuseCache[i] = this
        }

        /**
         * Doesn't respect indentation or quoting settings
         */
        fun writeTree(tree: JsonElement) {
            w.sb.append(tree.toString())
        }

        private var forceStr: Boolean = false

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) { // entry
                w.print(newMode.begin)
                w.indent()
            }

            if (mode == newMode) return this

            val cached = modeReuseCache[newMode.ordinal]
            if (cached != null) {
                return cached
            }

            return JsonOutput(newMode, w, modeReuseCache)
        }

        override fun endStructure(desc: SerialDescriptor) {
            if (mode.end != INVALID) {
                w.unIndent()
                w.nextItem()
                w.print(mode.end)
            }
        }

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (index == 0) return false
                    if (! w.writingFirst)
                        w.print(COMMA)
                    w.nextItem()
                }
                Mode.ENTRY, Mode.POLY -> {
                    if (index == 0)
                        forceStr = true
                    if (index == 1) {
                        w.print(if (mode == Mode.ENTRY) COLON else COMMA)
                        w.space()
                        forceStr = false
                    }
                }
                else -> {
                    if (! w.writingFirst)
                        w.print(COMMA)
                    w.nextItem()
                    encodeString(desc.getElementName(index))
                    w.print(COLON)
                    w.space()
                }
            }
            return true
        }

        override fun encodeNull() {
            w.print(NULL)
        }

        override fun encodeBoolean(value: Boolean) { if (forceStr) encodeString(value.toString()) else w.print(value) }
        override fun encodeByte(value: Byte) { if (forceStr) encodeString(value.toString()) else w.print(value) }
        override fun encodeShort(value: Short) { if (forceStr) encodeString(value.toString()) else w.print(value) }
        override fun encodeInt(value: Int) { if (forceStr) encodeString(value.toString()) else w.print(value) }
        override fun encodeLong(value: Long) { if (forceStr) encodeString(value.toString()) else w.print(value) }

        override fun encodeFloat(value: Float) {
            if (forceStr || !value.isFinite()) encodeString(value.toString()) else
                w.print(value)
        }

        override fun encodeDouble(value: Double) {
            if (forceStr || !value.isFinite()) encodeString(value.toString()) else
                w.print(value)
        }

        override fun encodeChar(value: Char) {
            encodeString(value.toString())
        }

        override fun encodeString(value: String) {
            if (unquoted && !mustBeQuoted(value)) {
                w.print(value)
            } else {
                w.printQuoted(value)
            }
        }

        override fun encodeNonSerializableValue(value: Any) {
            encodeString(value.toString())
        }
    }

    internal inner class Composer(internal val sb: StringBuilder) {
        private var level = 0
        var writingFirst = true
            private set
        fun indent() { writingFirst = true; level++ }
        fun unIndent() { level-- }

        fun nextItem() {
            writingFirst = false
            if (indented) {
                print("\n")
                repeat(level) { print(indent) }
            }
        }

        fun space() {
            if (indented)
                print(' ')
        }

        fun print(v: Char) = sb.append(v)
        fun print(v: String) = sb.append(v)

        fun print(v: Float) = sb.append(v)
        fun print(v: Double) = sb.append(v)
        fun print(v: Byte) = sb.append(v)
        fun print(v: Short) = sb.append(v)
        fun print(v: Int) = sb.append(v)
        fun print(v: Long) = sb.append(v)
        fun print(v: Boolean) = sb.append(v)

        fun printQuoted(value: String): Unit = sb.printQuoted(value)
    }

    inner class JsonInput internal constructor(private val mode: Mode, private val p: Parser) : ElementValueDecoder() {
        private var curIndex = 0
        private var entryIndex = 0

        init {
            context = this@JSON.context
        }

        fun readAsTree(): JsonElement = JsonTreeParser(p).read()

        override val updateMode: UpdateMode
            get() = this@JSON.updateMode

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) {
                p.requireTc(newMode.beginTc) { "Expected '${newMode.begin}, kind: ${desc.kind}'" }
                p.nextToken()
            }
            return when (newMode) {
                Mode.LIST, Mode.MAP, Mode.POLY -> JsonInput(newMode, p) // need fresh cur index
                else -> if (mode == newMode) this else
                    JsonInput(newMode, p) // todo: reuse instance per mode
            }
        }

        override fun endStructure(desc: SerialDescriptor) {
            if (mode.end != INVALID) {
                p.requireTc(mode.endTc) { "Expected '${mode.end}'" }
                p.nextToken()
            }
        }

        override fun decodeNotNullMark(): Boolean {
            return p.tc != TC_NULL
        }

        override fun decodeNull(): Nothing? {
            p.requireTc(TC_NULL) { "Expected 'null' literal" }
            p.nextToken()
            return null
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (true) {
                if (p.tc == TC_COMMA) p.nextToken()
                when (mode) {
                    Mode.LIST, Mode.MAP -> {
                        return if (!p.canBeginValue) READ_DONE else ++curIndex
                    }
                    Mode.POLY -> {
                        return when (entryIndex++) {
                            0 -> 0
                            1 -> 1
                            else -> {
                                entryIndex = 0
                                READ_DONE
                            }
                        }
                    }
                    Mode.ENTRY -> {
                        return when (entryIndex++) {
                            0 -> 0
                            1 -> {
                                p.requireTc(TC_COLON) { "Expected ':'" }
                                p.nextToken()
                                1
                            }
                            else -> {
                                entryIndex = 0
                                READ_DONE
                            }
                        }
                    }
                    else -> {
                        if (!p.canBeginValue) return READ_DONE
                        val key = p.takeStr()
                        p.requireTc(TC_COLON) { "Expected ':'" }
                        p.nextToken()
                        val ind = desc.getElementIndex(key)
                        if (ind != UNKNOWN_NAME)
                            return ind
                        if (!nonstrict)
                            throw SerializationException("Strict JSON encountered unknown key: $key")
                        else
                            p.skipElement()
                    }
                }
            }
        }

        override fun decodeBoolean(): Boolean = p.takeStr().toBoolean()
        override fun decodeByte(): Byte = p.takeStr().toByte()
        override fun decodeShort(): Short = p.takeStr().toShort()
        override fun decodeInt(): Int = p.takeStr().toInt()
        override fun decodeLong(): Long = p.takeStr().toLong()
        override fun decodeFloat(): Float = p.takeStr().toFloat()
        override fun decodeDouble(): Double = p.takeStr().toDouble()
        override fun decodeChar(): Char = p.takeStr().single()
        override fun decodeString(): String = p.takeStr()

        override fun <T : Enum<T>> decodeEnum(enumCreator: EnumCreator<T>): T = enumCreator.createFromName(p.takeStr())
    }
}

// ----------- JSON utilities -----------

internal enum class Mode(val begin: Char, val end: Char) {
    OBJ(BEGIN_OBJ, END_OBJ),
    LIST(BEGIN_LIST, END_LIST),
    MAP(BEGIN_OBJ, END_OBJ),
    POLY(BEGIN_LIST, END_LIST),
    ENTRY(INVALID, INVALID);

    val beginTc: Byte = charToTokenClass(begin)
    val endTc: Byte = charToTokenClass(end)
}

private fun switchMode(mode: Mode, desc: SerialDescriptor, typeParams: Array<out KSerializer<*>>): Mode =
    when (desc.kind) {
        UnionKind.POLYMORPHIC -> Mode.POLY
        StructureKind.LIST, StructureKind.SET -> Mode.LIST
        StructureKind.MAP -> {
            val keyKind = typeParams[0].descriptor.kind
            if (keyKind == PrimitiveKind.PRIMITIVE || keyKind == UnionKind.ENUM_KIND)
                Mode.MAP
            else Mode.LIST
        }
        StructureKind.ENTRY -> if (mode == Mode.MAP) Mode.ENTRY else Mode.OBJ
        else -> Mode.OBJ
    }

private fun mustBeQuoted(str: String): Boolean {
    if (str == NULL) return true
    for (ch in str) {
        if (charToTokenClass(ch) != TC_OTHER) return true
    }
    return false
}
