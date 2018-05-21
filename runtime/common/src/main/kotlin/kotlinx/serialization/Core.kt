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

import kotlinx.serialization.KInput.Companion.UNKNOWN_NAME
import kotlin.reflect.KClass

enum class SerialKind { // unit and object unused?
    CLASS, OBJECT, UNIT, SEALED, LIST, SET, MAP, ENTRY, POLYMORPHIC, PRIMITIVE, KIND_ENUM
}

interface SerialDescriptor {
    val name: String
    val kind: SerialKind
    fun getElementName(index: Int): String
    fun getElementIndex(name: String): Int
    fun getElementIndexOrThrow(name: String): Int {
        val i = getElementIndex(name)
        if (i == UNKNOWN_NAME) throw SerializationException("Unknown name '$name'")
        return i
    }

    fun getAnnotationsForIndex(index: Int): List<Annotation> = emptyList()
    val associatedFieldsCount: Int
        get() = 0
}

interface SerializationStrategy<in T> {
    fun serialize(output: Encoder, obj : T)
}

interface DeserializationStrategy<T> {
    fun deserialize(input: KInput): T
    fun patch(input: KInput, old: T): T
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

interface KSerializer<T>: SerializationStrategy<T>, DeserializationStrategy<T> {
    val serialClassDesc: SerialDescriptor

    override fun patch(input: KInput, old: T): T = throw UpdateNotSupportedException(serialClassDesc.name)
}

interface EnumCreator<E : Enum<E>> {
    fun createFromOrdinal(ordinal: Int): E
    fun createFromName(name: String): E
}

internal class LegacyEnumCreator<E : Enum<E>>(private val eClass: KClass<E>) : EnumCreator<E> {
    override fun createFromOrdinal(ordinal: Int): E {
        return enumFromOrdinal(eClass, ordinal)
    }

    override fun createFromName(name: String): E {
        return enumFromName(eClass, name)
    }
}

class SerializationConstructorMarker private constructor()

inline fun <reified T : Any> Encoder.encode(obj: T) { encode(T::class.serializer(), obj) }

fun <T : Any?> Encoder.encode(strategy: SerializationStrategy<T>, obj: T) { strategy.serialize(this, obj) }

fun <T : Any> Encoder.encodeNullable(strategy: SerializationStrategy<T>, obj: T?) {
    if (obj == null) {
        encodeNullValue()
    } else {
        encodeNotNullMark()
        strategy.serialize(this, obj)
    }
}

interface Encoder {
    var context: SerialContext?

    fun encodeValue(value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableValue(s, value)
        else encodeNonSerializableValue(value)
    }

    fun encodeNonSerializableValue(value: Any)

    fun encodeNotNullMark()
    fun encodeNullValue()

    fun encodeNullableValue(value: Any?)
    fun encodeUnitValue()
    fun encodeBooleanValue(value: Boolean)
    fun encodeByteValue(value: Byte)
    fun encodeShortValue(value: Short)
    fun encodeIntValue(value: Int)
    fun encodeLongValue(value: Long)
    fun encodeFloatValue(value: Float)
    fun encodeDoubleValue(value: Double)
    fun encodeCharValue(value: Char)
    fun encodeStringValue(value: String)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumValue(value)"))
    fun <T : Enum<T>> encodeEnumValue(enumClass: KClass<T>, value: T)

    fun <T : Enum<T>> encodeEnumValue(value: T)

    fun <T : Any?> encodeSerializableValue(saver: SerializationStrategy<T>, value: T) {
        saver.serialize(this, value)
    }

    fun <T : Any> encodeNullableSerializableValue(saver: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            encodeNullValue()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(saver, value)
        }
    }

    // composite value delimiter api (endStructure ends composite object)
    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder

    fun beginCollection(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) =
        beginStructure(desc, *typeParams)
}

interface StructureEncoder: Encoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder = this
    fun endStructure(desc: SerialDescriptor) {}

    // it is always invoked before encodeSerializableValue, shall return false if no need to encode (skip this value)
    fun encodeElement(desc: SerialDescriptor, index: Int): Boolean

    fun encodeElementValue(desc: SerialDescriptor, index: Int, value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableElementValue(desc, index, s, value)
        else encodeNonSerializableElementValue(desc, index, value)
    }

    fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?)

    fun encodeUnitElementValue(desc: SerialDescriptor, index: Int)
    fun encodeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean)
    fun encodeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte)
    fun encodeShortElementValue(desc: SerialDescriptor, index: Int, value: Short)
    fun encodeIntElementValue(desc: SerialDescriptor, index: Int, value: Int)
    fun encodeLongElementValue(desc: SerialDescriptor, index: Int, value: Long)
    fun encodeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float)
    fun encodeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double)
    fun encodeCharElementValue(desc: SerialDescriptor, index: Int, value: Char)
    fun encodeStringElementValue(desc: SerialDescriptor, index: Int, value: String)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumElementValue(desc, index, value)"))
    fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T)
    fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, value: T)

    fun <T : Any?> encodeSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T) {
        if (encodeElement(desc, index))
            encodeSerializableValue(saver, value)
    }

    fun encodeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> encodeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T?) {
        if (encodeElement(desc, index))
            encodeNullableSerializableValue(saver, value)
    }
}

abstract class KInput internal constructor() {

    var context: SerialContext? = null

    // ------- top-level API (use it) -------

    inline fun <reified T: Any> read(): T = this.read(T::class.serializer())
    fun <T : Any?> read(loader: DeserializationStrategy<T>): T = loader.deserialize(this)
    fun <T : Any> readNullable(loader: DeserializationStrategy<T>): T? = if (readNotNullMark()) read(loader) else readNullValue()

    // ------- low-level element value API for basic serializers -------

    // returns true if the following value is not null, false if not null
    abstract fun readNotNullMark(): Boolean
    abstract fun readNullValue(): Nothing? // consumes null, returns null, will be called when readNotNullMark() is false

    abstract fun readValue(): Any

    fun <T: Any> readValue(klass: KClass<T>): T {
        val s = context?.getSerializerByClass(klass)
        @Suppress("UNCHECKED_CAST")
        return if (s != null)
            readSerializableValue(s)
        else
            readValue() as T
    }
    abstract fun readNullableValue(): Any?
    abstract fun readUnitValue()
    abstract fun readBooleanValue(): Boolean
    abstract fun readByteValue(): Byte
    abstract fun readShortValue(): Short
    abstract fun readIntValue(): Int
    abstract fun readLongValue(): Long
    abstract fun readFloatValue(): Float
    abstract fun readDoubleValue(): Double
    abstract fun readCharValue(): Char
    abstract fun readStringValue(): String
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    abstract fun <T : Enum<T>> readEnumValue(enumClass: KClass<T> ): T

    abstract fun <T : Enum<T>> readEnumValue(enumCreator: EnumCreator<T>): T


    open fun <T : Any?> readSerializableValue(loader: DeserializationStrategy<T>): T = loader.deserialize(this)

    fun <T : Any> readNullableSerializableValue(loader: DeserializationStrategy<T?>): T? =
            if (readNotNullMark()) readSerializableValue(loader) else readNullValue()

    // -------------------------------------------------------------------------------------
    // methods below this line are invoked by compiler-generated KSerializer implementation

    // composite value delimiter api (endStructure ends composite object)
    open fun readBegin(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): KInput = this
    open fun readEnd(desc: SerialDescriptor) {}

    // readElement results
    companion object {
        const val READ_DONE = -1
        const val READ_ALL = -2
        const val UNKNOWN_NAME = -3
    }

    // returns either index or one of READ_XXX constants
    abstract fun readElement(desc: SerialDescriptor): Int

    abstract fun readElementValue(desc: SerialDescriptor, index: Int): Any

    fun readElementValue(desc: SerialDescriptor, index: Int, klass: KClass<*>): Any {
        val s = context?.getSerializerByClass(klass)
        return if (s != null) readSerializableElementValue(desc, index, s)
        else readElementValue(desc, index)
    }

    abstract fun readNullableElementValue(desc: SerialDescriptor, index: Int): Any?
    abstract fun readUnitElementValue(desc: SerialDescriptor, index: Int)
    abstract fun readBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean
    abstract fun readByteElementValue(desc: SerialDescriptor, index: Int): Byte
    abstract fun readShortElementValue(desc: SerialDescriptor, index: Int): Short
    abstract fun readIntElementValue(desc: SerialDescriptor, index: Int): Int
    abstract fun readLongElementValue(desc: SerialDescriptor, index: Int): Long
    abstract fun readFloatElementValue(desc: SerialDescriptor, index: Int): Float
    abstract fun readDoubleElementValue(desc: SerialDescriptor, index: Int): Double
    abstract fun readCharElementValue(desc: SerialDescriptor, index: Int): Char
    abstract fun readStringElementValue(desc: SerialDescriptor, index: Int): String
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(desc, index, enumLoader)"))
    abstract fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T

    abstract fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T


    abstract fun <T : Any?> readSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T
    abstract fun <T : Any> readNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T?

    open fun <T> updateSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>, old: T): T {
        return updateSerializableValue(loader, desc, old)
    }

    open fun <T: Any> updateNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>, old: T?): T? {
        return updateNullableSerializableValue(loader, desc, old)
    }

    open fun <T> updateSerializableValue(loader: DeserializationStrategy<T>, desc: SerialDescriptor, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            UpdateMode.OVERWRITE -> readSerializableValue(loader)
            UpdateMode.UPDATE -> loader.patch(this, old)
        }
    }

    open fun <T: Any> updateNullableSerializableValue(loader: DeserializationStrategy<T?>, desc: SerialDescriptor, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            updateMode == UpdateMode.OVERWRITE || old == null -> readNullableSerializableValue(loader)
            readNotNullMark() -> loader.patch(this, old)
            else -> readNullValue().let { old }
        }
    }

    open val updateMode: UpdateMode = UpdateMode.UPDATE
}
