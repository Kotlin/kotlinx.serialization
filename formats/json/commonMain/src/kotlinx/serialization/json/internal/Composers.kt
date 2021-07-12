/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*

@OptIn(ExperimentalSerializationApi::class)
internal open class Composer(@JvmField internal val sb: JsonStringBuilder, @JvmField internal val json: Json) {
    private var level = 0
    var writingFirst = true
        private set

    fun indent() {
        writingFirst = true
        level++
    }

    fun unIndent() {
        level--
    }

    fun nextItem() {
        writingFirst = false
        if (json.configuration.prettyPrint) {
            print("\n")
            repeat(level) { print(json.configuration.prettyPrintIndent) }
        }
    }

    fun space() {
        if (json.configuration.prettyPrint)
            print(' ')
    }

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

@ExperimentalUnsignedTypes
internal class ComposerForUnsignedNumbers(sb: JsonStringBuilder, json: Json) : Composer(sb, json) {
    override fun print(v: Int) {
        return super.print(v.toUInt().toString())
    }

    override fun print(v: Long) {
        return super.print(v.toULong().toString())
    }

    override fun print(v: Byte) {
        return super.print(v.toUByte().toString())
    }

    override fun print(v: Short) {
        return super.print(v.toUShort().toString())
    }
}
