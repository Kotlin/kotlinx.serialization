/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.math.*

/**
 * [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER]
 */
internal const val MAX_SAFE_INTEGER: Double = 9007199254740991.toDouble() // 2^53 - 1

/**
 * Converts native JavaScript objects into Kotlin ones, verifying their types.
 *
 * A result of `parse(nativeObj)` should be the same as
 * `kotlinx.serialization.json.Json.parse(kotlin.js.JSON.stringify(nativeObj))`.
 * This class also supports array-based polymorphism if the corresponding flag in [configuration] is set to `true`.
 * Does not support any other [Map] keys than [String].
 * Has limitation on [Long] type: any JS number that is greater than
 * [`abs(2^53-1)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER)
 * is considered to be imprecise and therefore can't be deserialized to [Long]. Either use [Double] type
 * for such values or pass them as strings using [LongAsStringSerializer] afterwards.
 *
 * Usage example:
 *
 * ```
 * @Serializable
 * data class Data(val a: Int)
 *
 * @Serializable
 * data class DataWrapper(val s: String, val d: Data?)
 *
 * val dyn: dynamic = js("""{s:"foo", d:{a:42}}""")
 * val parsed = DynamicObjectParser().parse(dyn, DataWrapper.serializer())
 * parsed == DataWrapper("foo", Data(42)) // true
 * ```
 */
public class DynamicObjectParser @OptIn(UnstableDefault::class) constructor(
    override val context: SerialModule = EmptyModule,
    internal val configuration: JsonConfiguration = JsonConfiguration.Default
) : SerialFormat {

    inline fun <reified T : Any> parse(value: dynamic): T = parse(value, context.getContextualOrDefault())

    /**
     * Deserializes given [obj] from dynamic form to type [T] using [deserializer].
     */
    public fun <T> parse(obj: dynamic, deserializer: DeserializationStrategy<T>): T =
        DynamicInput(obj).decodeSerializableValue(deserializer)

    private open inner class DynamicInput(val obj: dynamic) : NamedValueDecoder() {
        override val serializersModule: SerialModule
            get() = this@DynamicObjectParser.context

        override fun composeName(parentName: String, childName: String): String = childName

        private var pos = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (pos < descriptor.elementsCount) {
                val name = descriptor.getTag(pos++)
                if (obj[name] !== undefined) return pos - 1
            }
            return CompositeDecoder.DECODE_DONE
        }

        override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int =
            enumDescription.getElementIndexOrThrow(getByTag(tag) as String)

        protected open fun getByTag(tag: String): dynamic = obj[tag]

        override fun decodeTaggedChar(tag: String): Char {
            val o = getByTag(tag)
            return when (o) {
                is String -> if (o.length == 1) o[0] else throw SerializationException("$o can't be represented as Char")
                is Number -> o.toChar()
                else -> throw SerializationException("$o can't be represented as Char")
            }
        }

        override fun decodeTaggedLong(tag: String): Long {
            val obj = getByTag(tag)
            val number = obj as? Double ?: throw SerializationException("$obj is not a Number")
            val canBeConverted = number.isFinite() && floor(number) == number
            if (!canBeConverted)
                throw SerializationException("$number can't be represented as Long because it is not finite or has non-zero fractional part")
            val inBound = abs(number) <= MAX_SAFE_INTEGER
            if (!inBound)
                throw SerializationException("$number can't be deserialized to Long due to a potential precision loss")
            return number.toLong()
        }

        override fun decodeTaggedValue(tag: String): Any {
            val o = getByTag(tag) ?: throw MissingFieldException(tag)
            return o as Any
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            val o = getByTag(tag)
            if (o === undefined) throw MissingFieldException(tag)
            @Suppress("SENSELESS_COMPARISON") // null !== undefined !
            return o != null
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            val curObj = currentTagOrNull?.let { obj[it] } ?: obj
            val kind = when (descriptor.kind) {
                is PolymorphicKind -> {
                    if (configuration.useArrayPolymorphism) StructureKind.LIST
                    else StructureKind.MAP
                }
                else -> descriptor.kind
            }
            return when (kind) {
                StructureKind.LIST -> DynamicListInput(curObj)
                StructureKind.MAP -> DynamicMapInput(curObj)
                else -> DynamicInput(curObj)
            }
        }
    }

    private inner class DynamicMapInput(obj: dynamic) : DynamicInput(obj) {
        private val keys: dynamic = js("Object").keys(obj)
        private val size: Int = (keys.length as Int) * 2
        private var pos = -1

        override fun elementName(desc: SerialDescriptor, index: Int): String {
            val i = index / 2
            return keys[i] as String
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (pos < size - 1) {
                val i = pos++ / 2
                val name = keys[i] as String
                if (this.obj[name] !== undefined) return pos
            }
            return CompositeDecoder.DECODE_DONE
        }

        override fun getByTag(tag: String): dynamic {
            return if (pos % 2 == 0) tag else obj[tag]
        }
    }

    private inner class DynamicListInput(obj: dynamic) : DynamicInput(obj) {
        private val size = obj.length as Int
        private var pos = -1

        override fun elementName(desc: SerialDescriptor, index: Int): String = (index).toString()

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (pos < size - 1) {
                val o = obj[++pos]
                if (o !== undefined) return pos
            }
            return CompositeDecoder.DECODE_DONE
        }
    }
}
