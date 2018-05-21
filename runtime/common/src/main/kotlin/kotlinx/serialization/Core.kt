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

@Deprecated("Obsolete name.", ReplaceWith("SerialKind"))
typealias KSerialClassKind = SerialKind

enum class SerialKind { // unit and object unused?
    CLASS, OBJECT, UNIT, SEALED, LIST, SET, MAP, ENTRY, POLYMORPHIC, PRIMITIVE, KIND_ENUM
}

@Deprecated("Obsolete name.", ReplaceWith("SerialDescriptor"))
typealias KSerialClassDesc = SerialDescriptor

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

    fun getAnnotationsForClass(): List<Annotation> = emptyList()
}

@Deprecated("Obsolete name.", ReplaceWith("SerializationStrategy"))
typealias KSerialSaver<T> = SerializationStrategy<T>

interface SerializationStrategy<in T> {
    fun serialize(output: KOutput, obj : T)
}

@Deprecated("Obsolete name.", ReplaceWith("DeserializationStrategy"))
typealias KSerialLoader<T> = DeserializationStrategy<T>

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

abstract class KOutput internal constructor() {

    var context: SerialContext? = null

    // ------- top-level API (use it) -------

    fun <T : Any?> write(saver: SerializationStrategy<T>, obj: T) { saver.serialize(this, obj) }

    inline fun <reified T : Any> write(obj: T) { write(T::class.serializer(), obj) }

    fun <T : Any> writeNullable(saver: SerializationStrategy<T>, obj: T?) {
        if (obj == null) {
            writeNullValue()
        } else {
            writeNotNullMark()
            saver.serialize(this, obj)
        }
    }

    // ------- low-level element value API for basic serializers -------

    // it is always invoked before writeXxxValue, shall return false if no need to write (skip this value)
    abstract fun writeElement(desc: SerialDescriptor, index: Int): Boolean

    // will be followed by value
    abstract fun writeNotNullMark()

    // this is invoked after writeElement
    abstract fun writeNullValue()

    fun writeValue(value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) writeSerializableValue(s, value)
        else writeNonSerializableValue(value)
    }
    abstract fun writeNonSerializableValue(value: Any)

    abstract fun writeNullableValue(value: Any?): Unit
    abstract fun writeUnitValue()
    abstract fun writeBooleanValue(value: Boolean)
    abstract fun writeByteValue(value: Byte)
    abstract fun writeShortValue(value: Short)
    abstract fun writeIntValue(value: Int)
    abstract fun writeLongValue(value: Long)
    abstract fun writeFloatValue(value: Float)
    abstract fun writeDoubleValue(value: Double)
    abstract fun writeCharValue(value: Char)
    abstract fun writeStringValue(value: String)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeEnumValue(value)"))
    abstract fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T)

    abstract fun <T : Enum<T>> writeEnumValue(value: T)

    open fun <T : Any?> writeSerializableValue(saver: SerializationStrategy<T>, value: T) {
        saver.serialize(this, value)
    }

    fun <T : Any> writeNullableSerializableValue(saver: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            writeNullValue()
        } else {
            writeNotNullMark()
            writeSerializableValue(saver, value)
        }
    }

    // -------------------------------------------------------------------------------------
    // methods below this line are invoked by compiler-generated KSerializer implementation

    // composite value delimiter api (writeEnd ends composite object)
    open fun writeBegin(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): KOutput = this
    open fun writeBegin(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) = writeBegin(desc, *typeParams)
    open fun writeEnd(desc: SerialDescriptor) {}

    fun writeElementValue(desc: SerialDescriptor, index: Int, value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) writeSerializableElementValue(desc, index, s, value)
        else writeNonSerializableElementValue(desc, index, value)
    }
    abstract fun writeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?)

    abstract fun writeUnitElementValue(desc: SerialDescriptor, index: Int)
    abstract fun writeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean)
    abstract fun writeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte)
    abstract fun writeShortElementValue(desc: SerialDescriptor, index: Int, value: Short)
    abstract fun writeIntElementValue(desc: SerialDescriptor, index: Int, value: Int)
    abstract fun writeLongElementValue(desc: SerialDescriptor, index: Int, value: Long)
    abstract fun writeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float)
    abstract fun writeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double)
    abstract fun writeCharElementValue(desc: SerialDescriptor, index: Int, value: Char)
    abstract fun writeStringElementValue(desc: SerialDescriptor, index: Int, value: String)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeEnumElementValue(desc, index, value)"))
    abstract fun <T : Enum<T>> writeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T)

    abstract fun <T : Enum<T>> writeEnumElementValue(desc: SerialDescriptor, index: Int, value: T)

    fun <T : Any?> writeSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T) {
        if (writeElement(desc, index))
            writeSerializableValue(saver, value)
    }

    abstract fun writeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> writeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T?) {
        if (writeElement(desc, index))
            writeNullableSerializableValue(saver, value)
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

    // composite value delimiter api (writeEnd ends composite object)
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
