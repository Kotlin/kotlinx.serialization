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

enum class KSerialClassKind { // unit and object unused?
    CLASS, OBJECT, UNIT, SEALED, LIST, SET, MAP, ENTRY, POLYMORPHIC, PRIMITIVE, KIND_ENUM
}

interface KSerialClassDesc {
    val name: String
    val kind: KSerialClassKind
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

interface KSerialSaver<in T> {
    fun save(output: KOutput, obj : T)
}

interface KSerialLoader<T> {
    fun load(input: KInput): T
    fun update(input: KInput, old: T): T
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

interface KSerializer<T>: KSerialSaver<T>, KSerialLoader<T> {
    val serialClassDesc: KSerialClassDesc

    override fun update(input: KInput, old: T): T = throw UpdateNotSupportedException(serialClassDesc.name)
}

interface EnumLoader<E : Enum<E>> {
    fun loadByOrdinal(ordinal: Int): E
    fun loadByName(name: String): E
}

internal class LegacyEnumLoader<E : Enum<E>>(private val eClass: KClass<E>) : EnumLoader<E> {
    override fun loadByOrdinal(ordinal: Int): E {
        return enumFromOrdinal(eClass, ordinal)
    }

    override fun loadByName(name: String): E {
        return enumFromName(eClass, name)
    }
}

class SerializationConstructorMarker private constructor()

abstract class KOutput internal constructor() {

    var context: SerialContext? = null

    // ------- top-level API (use it) -------

    fun <T : Any?> write(saver: KSerialSaver<T>, obj: T) { saver.save(this, obj) }

    inline fun <reified T : Any> write(obj: T) { write(T::class.serializer(), obj) }

    fun <T : Any> writeNullable(saver: KSerialSaver<T>, obj: T?) {
        if (obj == null) {
            writeNullValue()
        } else {
            writeNotNullMark()
            saver.save(this, obj)
        }
    }

    // ------- low-level element value API for basic serializers -------

    // it is always invoked before writeXxxValue, shall return false if no need to write (skip this value)
    abstract fun writeElement(desc: KSerialClassDesc, index: Int): Boolean

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

    open fun <T : Any?> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        saver.save(this, value)
    }

    fun <T : Any> writeNullableSerializableValue(saver: KSerialSaver<T>, value: T?) {
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
    open fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput = this
    open fun writeBegin(desc: KSerialClassDesc, collectionSize: Int, vararg typeParams: KSerializer<*>) = writeBegin(desc, *typeParams)
    open fun writeEnd(desc: KSerialClassDesc) {}

    fun writeElementValue(desc: KSerialClassDesc, index: Int, value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) writeSerializableElementValue(desc, index, s, value)
        else writeNonSerializableElementValue(desc, index, value)
    }
    abstract fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?)

    abstract fun writeUnitElementValue(desc: KSerialClassDesc, index: Int)
    abstract fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean)
    abstract fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte)
    abstract fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short)
    abstract fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int)
    abstract fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long)
    abstract fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float)
    abstract fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double)
    abstract fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char)
    abstract fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeEnumElementValue(desc, index, value)"))
    abstract fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T)

    abstract fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, value: T)

    fun <T : Any?> writeSerializableElementValue(desc: KSerialClassDesc, index: Int, saver: KSerialSaver<T>, value: T) {
        if (writeElement(desc, index))
            writeSerializableValue(saver, value)
    }

    abstract fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any)

    fun <T : Any> writeNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, saver: KSerialSaver<T>, value: T?) {
        if (writeElement(desc, index))
            writeNullableSerializableValue(saver, value)
    }
}

abstract class KInput internal constructor() {

    var context: SerialContext? = null

    // ------- top-level API (use it) -------

    inline fun <reified T: Any> read(): T = this.read(T::class.serializer())
    fun <T : Any?> read(loader: KSerialLoader<T>): T = loader.load(this)
    fun <T : Any> readNullable(loader: KSerialLoader<T>): T? = if (readNotNullMark()) read(loader) else readNullValue()

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

    abstract fun <T : Enum<T>> readEnumValue(enumLoader: EnumLoader<T>): T


    open fun <T : Any?> readSerializableValue(loader: KSerialLoader<T>): T = loader.load(this)

    fun <T : Any> readNullableSerializableValue(loader: KSerialLoader<T?>): T? =
            if (readNotNullMark()) readSerializableValue(loader) else readNullValue()

    // -------------------------------------------------------------------------------------
    // methods below this line are invoked by compiler-generated KSerializer implementation

    // composite value delimiter api (writeEnd ends composite object)
    open fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput = this
    open fun readEnd(desc: KSerialClassDesc) {}

    // readElement results
    companion object {
        const val READ_DONE = -1
        const val READ_ALL = -2
        const val UNKNOWN_NAME = -3
    }

    // returns either index or one of READ_XXX constants
    abstract fun readElement(desc: KSerialClassDesc): Int

    abstract fun readElementValue(desc: KSerialClassDesc, index: Int): Any

    fun readElementValue(desc: KSerialClassDesc, index: Int, klass: KClass<*>): Any {
        val s = context?.getSerializerByClass(klass)
        return if (s != null) readSerializableElementValue(desc, index, s)
        else readElementValue(desc, index)
    }

    abstract fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any?
    abstract fun readUnitElementValue(desc: KSerialClassDesc, index: Int)
    abstract fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean
    abstract fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte
    abstract fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short
    abstract fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int
    abstract fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long
    abstract fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float
    abstract fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double
    abstract fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char
    abstract fun readStringElementValue(desc: KSerialClassDesc, index: Int): String
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(desc, index, enumLoader)"))
    abstract fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T

    abstract fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumLoader: EnumLoader<T>): T


    abstract fun <T : Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T
    abstract fun <T : Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T?

    open fun <T> updateSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>, old: T): T {
        return updateSerializableValue(loader, desc, old)
    }

    open fun <T: Any> updateNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>, old: T?): T? {
        return updateNullableSerializableValue(loader, desc, old)
    }

    open fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            UpdateMode.OVERWRITE -> readSerializableValue(loader)
            UpdateMode.UPDATE -> loader.update(this, old)
        }
    }

    open fun <T: Any> updateNullableSerializableValue(loader: KSerialLoader<T?>, desc: KSerialClassDesc, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            updateMode == UpdateMode.OVERWRITE || old == null -> readNullableSerializableValue(loader)
            readNotNullMark() -> loader.update(this, old)
            else -> readNullValue().let { old }
        }
    }

    open val updateMode: UpdateMode = UpdateMode.UPDATE
}
