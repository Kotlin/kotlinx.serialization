/*
 *  Copyright 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.PolymorphicClassDesc

object PolymorphicSerializer : KSerializer<Any> {

    override val serialClassDesc: KSerialClassDesc
        get() = PolymorphicClassDesc

    override fun save(output: KOutput, obj: Any) {
        val saver = serializerByValue(obj, output.context)
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc)
        output.writeStringElementValue(serialClassDesc, 0, saver.serialClassDesc.name)
        output.writeSerializableElementValue(serialClassDesc, 1, saver, obj)
        output.writeEnd(serialClassDesc)
    }

    override fun load(input: KInput): Any {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (input.readElement(serialClassDesc)) {
                KInput.READ_ALL -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                    val loader = serializerByClass<Any>(klassName, input.context)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                    break@mainLoop
                }
                KInput.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = serializerByClass<Any>(klassName, input.context)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.readEnd(serialClassDesc)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }
}