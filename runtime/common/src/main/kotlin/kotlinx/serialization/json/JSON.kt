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
import kotlinx.serialization.internal.*

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
        output.write(saver, obj)
        return sb.toString()
    }

    inline fun <reified T : Any> stringify(obj: T): String = stringify(context.klassSerializer(T::class), obj)

    fun <T> parse(loader: DeserializationStrategy<T>, str: String): T {
        val parser = Parser(str)
        val input = JsonInput(Mode.OBJ, parser)
        val result = input.read(loader)
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

    inner class JsonOutput internal constructor(private val mode: Mode, private val w: Composer, private val modeReuseCache: Array<JsonOutput?>) : ElementValueOutput() {
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

        override fun writeBegin(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): KOutput {
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

        override fun writeEnd(desc: SerialDescriptor) {
            if (mode.end != INVALID) {
                w.unIndent()
                w.nextItem()
                w.print(mode.end)
            }
        }

        override fun writeElement(desc: SerialDescriptor, index: Int): Boolean {
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
                    writeStringValue(desc.getElementName(index))
                    w.print(COLON)
                    w.space()
                }
            }
            return true
        }

        override fun writeNullValue() {
            w.print(NULL)
        }

        override fun writeBooleanValue(value: Boolean) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeByteValue(value: Byte) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeShortValue(value: Short) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeIntValue(value: Int) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeLongValue(value: Long) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }

        override fun writeFloatValue(value: Float) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeDoubleValue(value: Double) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeCharValue(value: Char) {
            writeStringValue(value.toString())
        }

        override fun writeStringValue(value: String) {
            if (unquoted && !mustBeQuoted(value)) {
                w.print(value)
            } else {
                w.printQuoted(value)
            }
        }

        override fun writeNonSerializableValue(value: Any) {
            writeStringValue(value.toString())
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

    inner class JsonInput internal constructor(private val mode: Mode, private val p: Parser) : ElementValueInput() {
        private var curIndex = 0
        private var entryIndex = 0

        init {
            context = this@JSON.context
        }

        fun readAsTree(): JsonElement = JsonTreeParser(p).read()

        override val updateMode: UpdateMode
            get() = this@JSON.updateMode

        override fun readBegin(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): KInput {
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

        override fun readEnd(desc: SerialDescriptor) {
            if (mode.end != INVALID) {
                p.requireTc(mode.endTc) { "Expected '${mode.end}'" }
                p.nextToken()
            }
        }

        override fun readNotNullMark(): Boolean {
            return p.tc != TC_NULL
        }

        override fun readNullValue(): Nothing? {
            p.requireTc(TC_NULL) { "Expected 'null' literal" }
            p.nextToken()
            return null
        }

        override fun readElement(desc: SerialDescriptor): Int {
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

        override fun readBooleanValue(): Boolean = p.takeStr().toBoolean()
        override fun readByteValue(): Byte = p.takeStr().toByte()
        override fun readShortValue(): Short = p.takeStr().toShort()
        override fun readIntValue(): Int = p.takeStr().toInt()
        override fun readLongValue(): Long = p.takeStr().toLong()
        override fun readFloatValue(): Float = p.takeStr().toFloat()
        override fun readDoubleValue(): Double = p.takeStr().toDouble()
        override fun readCharValue(): Char = p.takeStr().single()
        override fun readStringValue(): String = p.takeStr()

        override fun <T : Enum<T>> readEnumValue(enumCreator: EnumCreator<T>): T = enumCreator.createFromName(p.takeStr())
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
        SerialKind.POLYMORPHIC -> Mode.POLY
        SerialKind.LIST, SerialKind.SET -> Mode.LIST
        SerialKind.MAP -> {
            val keyKind = typeParams[0].serialClassDesc.kind
            if (keyKind == SerialKind.PRIMITIVE || keyKind == SerialKind.KIND_ENUM)
                Mode.MAP
            else Mode.LIST
        }
        SerialKind.ENTRY -> if (mode == Mode.MAP) Mode.ENTRY else Mode.OBJ
        else -> Mode.OBJ
    }

private fun mustBeQuoted(str: String): Boolean {
    if (str == NULL) return true
    for (ch in str) {
        if (charToTokenClass(ch) != TC_OTHER) return true
    }
    return false
}
