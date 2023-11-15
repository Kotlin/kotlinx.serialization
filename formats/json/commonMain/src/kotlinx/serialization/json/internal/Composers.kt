/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)
package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*

internal fun Composer(sb: InternalJsonWriter, json: Json): Composer =
    if (json.configuration.prettyPrint) ComposerWithPrettyPrint(sb, json) else Composer(sb)

@OptIn(ExperimentalSerializationApi::class)
internal open class Composer(@JvmField internal val writer: InternalJsonWriter) {
    var writingFirst = true
        protected set

    open fun indent() {
        writingFirst = true
    }

    open fun unIndent() = Unit

    open fun nextItem() {
        writingFirst = false
    }

    open fun nextItemIfNotFirst() {
        writingFirst = false
    }

    open fun space() = Unit

    fun print(v: Char) = writer.writeChar(v)
    fun print(v: String) = writer.write(v)
    open fun print(v: Float) = writer.write(v.toString())
    open fun print(v: Double) = writer.write(v.toString())
    open fun print(v: Byte) = writer.writeLong(v.toLong())
    open fun print(v: Short) = writer.writeLong(v.toLong())
    open fun print(v: Int) = writer.writeLong(v.toLong())
    open fun print(v: Long) = writer.writeLong(v)
    open fun print(v: Boolean) = writer.write(v.toString())
    open fun printQuoted(value: String) = writer.writeQuoted(value)
}

@SuppressAnimalSniffer // Long(Integer).toUnsignedString(long)
internal class ComposerForUnsignedNumbers(writer: InternalJsonWriter, private val forceQuoting: Boolean) : Composer(writer) {
    override fun print(v: Int) {
        if (forceQuoting) printQuoted(v.toUInt().toString()) else print(v.toUInt().toString())
    }

    override fun print(v: Long) {
        if (forceQuoting) printQuoted(v.toULong().toString()) else print(v.toULong().toString())
    }

    override fun print(v: Byte) {
        if (forceQuoting) printQuoted(v.toUByte().toString()) else print(v.toUByte().toString())
    }

    override fun print(v: Short) {
        if (forceQuoting) printQuoted(v.toUShort().toString()) else print(v.toUShort().toString())
    }
}

@SuppressAnimalSniffer
internal class ComposerForUnquotedLiterals(writer: InternalJsonWriter, private val forceQuoting: Boolean) : Composer(writer) {
    override fun printQuoted(value: String) {
        if (forceQuoting) super.printQuoted(value) else super.print(value)
    }
}

internal class ComposerWithPrettyPrint(
    writer: InternalJsonWriter,
    private val json: Json
) : Composer(writer) {
    private var level = 0

    override fun indent() {
        writingFirst = true
        level++
    }

    override fun unIndent() {
        level--
    }

    override fun nextItem() {
        writingFirst = false
        print("\n")
        repeat(level) { print(json.configuration.prettyPrintIndent) }
    }

    override fun nextItemIfNotFirst() {
        if (writingFirst) writingFirst = false
        else nextItem()
    }

    override fun space() {
        print(' ')
    }
}
