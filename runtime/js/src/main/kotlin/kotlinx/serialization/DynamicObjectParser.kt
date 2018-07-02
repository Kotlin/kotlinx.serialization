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

import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE

class DynamicObjectParser(val context: SerialContext? = null) {
    inline fun <reified T : Any> parse(obj: dynamic): T = parse(obj, context.klassSerializer(T::class))
    fun <T> parse(obj: dynamic, loader: DeserializationStrategy<T>): T = DynamicInput(obj).decode(loader)

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

        override fun <E : Enum<E>> decodeTaggedEnum(tag: String, enumCreator: EnumCreator<E>) =
                enumCreator.createFromName(getByTag(tag) as String)

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
                StructureKind.LIST, StructureKind.SET -> DynamicListInput(curObj)
                StructureKind.MAP -> DynamicMapInput(curObj)
                StructureKind.ENTRY -> DynamicMapValueInput(curObj, currentTag)
                else -> DynamicInput(curObj)
            }
        }
    }

    private inner class DynamicMapValueInput(obj: dynamic, val cTag: String): DynamicInput(obj) {
        init {
            this.context = this@DynamicObjectParser.context
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int = READ_ALL

        override fun getByTag(tag: String): dynamic {
            return if (tag == "key") cTag else obj
        }
    }

    private inner class DynamicMapInput(obj: dynamic): DynamicInput(obj) {
        init {
            this.context = this@DynamicObjectParser.context
        }

        private val keys: dynamic = js("Object").keys(obj)
        private val size: Int = keys.length as Int
        private var pos = 0

        override fun elementName(desc: SerialDescriptor, index: Int): String {
            val i = index - 1
            return keys[i]
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size) {
                val i = pos++
                val name = keys[i] as String
                if (this.obj[name] !== undefined) return pos
            }
            return READ_DONE
        }
    }

    private inner class DynamicListInput(obj: dynamic): DynamicInput(obj) {
        init {
            this.context = this@DynamicObjectParser.context
        }

        private val size = obj.length as Int
        private var pos = 0 // 0st element is SIZE. use it?

        override fun elementName(desc: SerialDescriptor, index: Int): String = (index - 1).toString()

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size) {
                val o = obj[pos++]
                if (o !== undefined) return pos
            }
            return READ_DONE
        }
    }
}
