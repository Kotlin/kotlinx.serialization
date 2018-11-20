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

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.context.*
import kotlinx.serialization.internal.EnumDescriptor

class DynamicObjectParser(): AbstractSerialFormat() {
    @ImplicitReflectionSerializer
    inline fun <reified T : Any> parse(obj: dynamic): T = parse(obj, context.getOrDefault(T::class))

    fun <T> parse(obj: dynamic, deserializer: DeserializationStrategy<T>): T = DynamicInput(obj).decode(deserializer)

    private open inner class DynamicInput(val obj: dynamic) : NamedValueDecoder() {
        init {
            this.context = this@DynamicObjectParser.context
        }
        override fun composeName(parentName: String, childName: String): String = childName

        private var pos = 0

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < desc.elementsCount) {
                val name = desc.getTag(pos++)
                if (obj[name] !== undefined) return pos - 1
            }
            return READ_DONE
        }

        override fun decodeTaggedEnum(tag: String, enumDescription: EnumDescriptor): Int =
                enumDescription.getElementIndex(getByTag(tag) as String)

        protected open fun getByTag(tag: String): dynamic = obj[tag]

        override fun decodeTaggedChar(tag: String): Char {
            val o = getByTag(tag)
            return when(o) {
                is String -> if (o.length == 1) o[0] else throw SerializationException("$o can't be represented as Char")
                is Number -> o.toChar()
                else -> throw SerializationException("$o can't be represented as Char")
            }
        }

        override fun decodeTaggedValue(tag: String): Any {
            val o = getByTag(tag) ?: throw MissingFieldException(tag)
            return o
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            val o = getByTag(tag)
            if (o === undefined) throw MissingFieldException(tag)
            @Suppress("SENSELESS_COMPARISON") // null !== undefined !
            return o != null
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val curObj = currentTagOrNull?.let { obj[it] } ?: obj
            return when (desc.kind) {
                StructureKind.LIST -> DynamicListInput(curObj)
                StructureKind.MAP -> DynamicMapInput(curObj)
                else -> DynamicInput(curObj)
            }
        }
    }

    private inner class DynamicMapInput(obj: dynamic): DynamicInput(obj) {
        init {
            this.context = this@DynamicObjectParser.context
        }

        private val keys: dynamic = js("Object").keys(obj)
        private val size: Int = (keys.length as Int) * 2
        private var pos = -1

        override fun elementName(desc: SerialDescriptor, index: Int): String {
            val i = index / 2
            return keys[i] as String
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size - 1) {
                val i = pos++ / 2
                val name = keys[i] as String
                if (this.obj[name] !== undefined) return pos
            }
            return READ_DONE
        }

        override fun getByTag(tag: String): dynamic {
            return if (pos % 2 == 0) tag else obj[tag]
        }
    }

    private inner class DynamicListInput(obj: dynamic): DynamicInput(obj) {
        init {
            this.context = this@DynamicObjectParser.context
        }

        private val size = obj.length as Int
        private var pos = -1

        override fun elementName(desc: SerialDescriptor, index: Int): String = (index).toString()

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size - 1) {
                val o = obj[++pos]
                if (o !== undefined) return pos
            }
            return READ_DONE
        }
    }
}
