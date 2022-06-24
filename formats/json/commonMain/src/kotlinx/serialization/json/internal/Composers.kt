/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)
package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*

internal fun Composer(sb: JsonStringBuilder, json: Json): Composer =
    if (json.configuration.prettyPrint) ComposerWithPrettyPrint(sb, json) else Composer(sb)

@OptIn(ExperimentalSerializationApi::class)
internal open class Composer(@JvmField internal val sb: JsonStringBuilder) {
    var writingFirst = true
        protected set

    open fun indent() {
        writingFirst = true
    }

    open fun unIndent() = Unit

    open fun nextItem() {
        writingFirst = false
    }

    open fun space() = Unit

    fun print(v: Char) = sb.append(v)
    fun print(v: String) = sb.append(v)
    open fun print(v: Float) = sb.append(v.toString())
    open fun print(v: Double) = sb.append(v.toString())
    open fun print(v: Byte) = sb.append(v.toLong())
    open fun print(v: Short) = sb.append(v.toLong())
    open fun print(v: Int) = sb.append(v.toLong())
    open fun print(v: Long) = sb.append(v)
    open fun print(v: Boolean) = sb.append(v.toString())
    fun printQuoted(value: String): Unit = sb.appendQuoted(value)
}

internal class ComposerForUnsignedNumbers(sb: JsonStringBuilder, private val forceQuoting: Boolean) : Composer(sb) {
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

internal class ComposerWithPrettyPrint(
    sb: JsonStringBuilder,
    private val json: Json
) : Composer(sb) {
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

    override fun space() {
        print(' ')
    }
}
