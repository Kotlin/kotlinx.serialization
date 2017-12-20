/*
 * Copyright 2017 JetBrains s.r.o.
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
import kotlinx.serialization.internal.UnitSerializer
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Serializable(
        val with: KClass<out KSerializer<*>> = KSerializer::class // it means -- use default serializer by default
)

@Target(AnnotationTarget.CLASS)
annotation class Serializer(
        val forClass: KClass<*> // what class to create serializer for
)

// additional optional annotations

@Target(AnnotationTarget.PROPERTY)
annotation class SerialName(val value: String)

@Target(AnnotationTarget.PROPERTY)
annotation class Optional()

@Target(AnnotationTarget.PROPERTY)
annotation class Transient()

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfo()

enum class KSerialClassKind { // unit and object unused?
    CLASS, OBJECT, UNIT, SEALED, LIST, SET, MAP, ENTRY, POLYMORPHIC, PRIMITIVE, ENUM
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
}

interface KSerialSaver<in T> {
    fun save(output: KOutput, obj : T)
}

interface KSerialLoader<T> {
    fun load(input: KInput): T
    fun update(input: KInput, old: T): T
}

interface KSerializer<T>: KSerialSaver<T>, KSerialLoader<T> {
    val serialClassDesc: KSerialClassDesc

    override fun update(input: KInput, old: T): T = throw UpdateNotSupportedException(serialClassDesc.name)
}

class SerializationConstructorMarker private constructor()

// ====== Exceptions ======

open class SerializationException(s: String) : RuntimeException(s)

class MissingFieldException(fieldName: String) : SerializationException("Field $fieldName is required, but it was missing")
class UnknownFieldException(index: Int): SerializationException("Unknown field for index $index")

// ========================================================================================================================

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
    abstract fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T)

    inline fun <reified T : Enum<T>> writeEnumValue(value: T) = writeEnumValue(T::class, value)

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
    abstract fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T)

    inline fun <reified T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, value: T) {
        writeEnumElementValue(desc, index, T::class, value)
    }

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
    abstract fun <T : Enum<T>> readEnumValue(enumClass: KClass<T> ): T

    inline fun <reified T : Enum<T>> readEnumValue(): T = readEnumValue(T::class)

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
    abstract fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T

    inline fun <reified T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int): T =
            readEnumElementValue(desc, index, T::class)

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
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            UpdateMode.OVERWRITE -> readNullableSerializableValue(loader)
            UpdateMode.UPDATE -> loader.update(this, old)
        }
    }

    open val updateMode: UpdateMode = UpdateMode.UPDATE
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

class UpdateNotSupportedException(className: String): SerializationException("Update is not supported for $className")

// ========================================================================================================================

open class ElementValueOutput : KOutput() {
    // ------- implementation API -------

    // it is always invoked before writeXxxValue
    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean = true

    // override for a special representation of nulls if needed (empty object by default)
    override fun writeNotNullMark() {}

    override fun writeNonSerializableValue(value: Any) {
        throw SerializationException("\"$value\" has no serializer")
    }

    override final fun writeNullableValue(value: Any?) {
        if (value == null) {
            writeNullValue()
        } else {
            writeNotNullMark()
            writeValue(value)
        }
    }

    override fun writeNullValue() {
        throw SerializationException("null is not supported")
    }
    override fun writeUnitValue() {
        val output = writeBegin(UnitSerializer.serialClassDesc); output.writeEnd(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based output, override for performance and custom type representations
    override fun writeBooleanValue(value: Boolean) = writeValue(value)
    override fun writeByteValue(value: Byte) = writeValue(value)
    override fun writeShortValue(value: Short) = writeValue(value)
    override fun writeIntValue(value: Int) = writeValue(value)
    override fun writeLongValue(value: Long) = writeValue(value)
    override fun writeFloatValue(value: Float) = writeValue(value)
    override fun writeDoubleValue(value: Double) = writeValue(value)
    override fun writeCharValue(value: Char) = writeValue(value)
    override fun writeStringValue(value: String) = writeValue(value)
    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) = writeValue(value)

    // -------------------------------------------------------------------------------------

    override final fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) { if (writeElement(desc, index)) writeValue(value) }
    override final fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) { if (writeElement(desc, index)) writeNullableValue(value) }
    override final fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) { if (writeElement(desc, index)) writeUnitValue() }
    override final fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) { if (writeElement(desc, index)) writeBooleanValue(value) }
    override final fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) { if (writeElement(desc, index)) writeByteValue(value) }
    override final fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) { if (writeElement(desc, index)) writeShortValue(value) }
    override final fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) { if (writeElement(desc, index)) writeIntValue(value) }
    override final fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) { if (writeElement(desc, index)) writeLongValue(value) }
    override final fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) { if (writeElement(desc, index)) writeFloatValue(value) }
    override final fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) { if (writeElement(desc, index)) writeDoubleValue(value) }
    override final fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) { if (writeElement(desc, index)) writeCharValue(value) }
    override final fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) { if (writeElement(desc, index)) writeStringValue(value) }
    override final fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T) { if (writeElement(desc, index)) writeEnumValue(enumClass, value) }


}

open class ElementValueInput : KInput() {
    // ------- implementation API -------

    // unordered read api, override to read props in arbitrary order
    override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

    // returns true if the following value is not null, false if not null
    override fun readNotNullMark(): Boolean = true
    override fun readNullValue(): Nothing? = null

    override fun readValue(): Any {
        throw SerializationException("Any type is not supported")
    }
    override fun readNullableValue(): Any? = if (readNotNullMark()) readValue() else readNullValue()
    override fun readUnitValue() {
        val reader = readBegin(UnitSerializer.serialClassDesc); reader.readEnd(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based input, override for performance and custom type representations
    override fun readBooleanValue(): Boolean = readValue() as Boolean
    override fun readByteValue(): Byte = readValue() as Byte
    override fun readShortValue(): Short = readValue() as Short
    override fun readIntValue(): Int = readValue() as Int
    override fun readLongValue(): Long = readValue() as Long
    override fun readFloatValue(): Float = readValue() as Float
    override fun readDoubleValue(): Double = readValue() as Double
    override fun readCharValue(): Char = readValue() as Char
    override fun readStringValue(): String = readValue() as String

    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T> ): T =
            readValue() as T

    // -------------------------------------------------------------------------------------

    override final fun readElementValue(desc: KSerialClassDesc, index: Int): Any = readValue()
    override final fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? = readNullableValue()
    override final fun readUnitElementValue(desc: KSerialClassDesc, index: Int) = readUnitValue()
    override final fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean = readBooleanValue()
    override final fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte = readByteValue()
    override final fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short = readShortValue()
    override final fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int = readIntValue()
    override final fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long = readLongValue()
    override final fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float = readFloatValue()
    override final fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double = readDoubleValue()
    override final fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char = readCharValue()
    override final fun readStringElementValue(desc: KSerialClassDesc, index: Int): String = readStringValue()
    override final fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T = readEnumValue(enumClass)

    override final fun <T: Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T =
            readSerializableValue(loader)

    override final fun <T: Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? =
            readNullableSerializableValue(loader)
}

// ========================================================================================================================

open class ValueTransformer {
    // ------- top-level API (use it) -------

    fun <T> transform(serializer: KSerializer<T>, obj: T): T {
        val output = Output()
        output.write(serializer, obj)
        val input = Input(output.list)
        return input.read(serializer)
    }

    inline fun <reified T : Any> transform(obj: T): T = transform(T::class.serializer(), obj)

    // ------- override to define transformations -------

    open fun transformBooleanValue(desc: KSerialClassDesc, index: Int, value: Boolean) = value
    open fun transformByteValue(desc: KSerialClassDesc, index: Int, value: Byte) = value
    open fun transformShortValue(desc: KSerialClassDesc, index: Int, value: Short) = value
    open fun transformIntValue(desc: KSerialClassDesc, index: Int, value: Int) = value
    open fun transformLongValue(desc: KSerialClassDesc, index: Int, value: Long) = value
    open fun transformFloatValue(desc: KSerialClassDesc, index: Int, value: Float) = value
    open fun transformDoubleValue(desc: KSerialClassDesc, index: Int, value: Double) = value
    open fun transformCharValue(desc: KSerialClassDesc, index: Int, value: Char) = value
    open fun transformStringValue(desc: KSerialClassDesc, index: Int, value: String) = value

    open fun <T : Enum<T>> transformEnumValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T): T = value

    open fun isRecursiveTransform(): Boolean = true

    // ---------------

    private inner class Output : KOutput() {
        internal val list = arrayListOf<Any?>()

        override fun writeNullableValue(value: Any?) {
            list.add(value)
        }

        override fun writeElement(desc: KSerialClassDesc, index: Int) = true
        override fun writeNotNullMark() {}
        override fun writeNullValue() { writeNullableValue(null) }
        override fun writeNonSerializableValue(value: Any) { writeNullableValue(value) }
        override fun writeUnitValue() { writeNullableValue(Unit) }
        override fun writeBooleanValue(value: Boolean) { writeNullableValue(value) }
        override fun writeByteValue(value: Byte) { writeNullableValue(value) }
        override fun writeShortValue(value: Short) { writeNullableValue(value) }
        override fun writeIntValue(value: Int) { writeNullableValue(value) }
        override fun writeLongValue(value: Long) { writeNullableValue(value) }
        override fun writeFloatValue(value: Float) { writeNullableValue(value) }
        override fun writeDoubleValue(value: Double) { writeNullableValue(value) }
        override fun writeCharValue(value: Char) { writeNullableValue(value) }
        override fun writeStringValue(value: String) { writeNullableValue(value) }
        override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) { writeNullableValue(value) }

        override fun <T : Any?> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
            if (isRecursiveTransform()) {
                saver.save(this, value)
            } else
                writeNullableValue(value)
        }

        // ---------------

        override fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) { writeNullableValue(value) }
        override fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) = writeNullableValue(value)
        override fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) = writeNullableValue(Unit)
        override fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) = writeNullableValue(value)
        override fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) = writeNullableValue(value)
        override fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) = writeNullableValue(value)
        override fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) = writeNullableValue(value)
        override fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) = writeNullableValue(value)
        override fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) = writeNullableValue(value)
        override fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) = writeNullableValue(value)
        override fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) = writeNullableValue(value)
        override fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) = writeNullableValue(value)

        override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T) =
                writeNullableValue(value)
    }

    private inner class Input(private val list: List<Any?>) : KInput() {
        private var index = 0
        private var curDesc: KSerialClassDesc? = null
        private var curIndex: Int = 0

        private fun cur(desc: KSerialClassDesc, index: Int) {
            curDesc = desc
            curIndex = index
        }

        override fun readNotNullMark(): Boolean = list[index] != null
        override fun readNullValue(): Nothing? { index++; return null }
        override fun readValue(): Any = list[index++]!!
        override fun readNullableValue(): Any? = list[index++]
        override fun readUnitValue(): Unit { index++ }

        override fun readBooleanValue(): Boolean = transformBooleanValue(curDesc!!, curIndex, readValue() as Boolean)
        override fun readByteValue(): Byte = transformByteValue(curDesc!!, curIndex, readValue() as Byte)
        override fun readShortValue(): Short = transformShortValue(curDesc!!, curIndex, readValue() as Short)
        override fun readIntValue(): Int = transformIntValue(curDesc!!, curIndex, readValue() as Int)
        override fun readLongValue(): Long = transformLongValue(curDesc!!, curIndex, readValue() as Long)
        override fun readFloatValue(): Float = transformFloatValue(curDesc!!, curIndex, readValue() as Float)
        override fun readDoubleValue(): Double = transformDoubleValue(curDesc!!, curIndex, readValue() as Double)
        override fun readCharValue(): Char = transformCharValue(curDesc!!, curIndex, readValue() as Char)
        override fun readStringValue(): String = transformStringValue(curDesc!!, curIndex, readValue() as String)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T> ): T =
                transformEnumValue(curDesc!!, curIndex, enumClass, readValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> readSerializableValue(loader: KSerialLoader<T>): T {
            if (isRecursiveTransform())
                return loader.load(this)
            else
                return readValue() as T
        }

        // ---------------

        override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

        override fun readElementValue(desc: KSerialClassDesc, index: Int): Any {
            cur(desc, index); return readValue()
        }

        override fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? {
            cur(desc, index); return readNullableValue()
        }
        override fun readUnitElementValue(desc: KSerialClassDesc, index: Int) { cur(desc, index); return readUnitValue() }
        override fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean { cur(desc, index); return readBooleanValue() }
        override fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte { cur(desc, index); return readByteValue() }
        override fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short { cur(desc, index); return readShortValue() }
        override fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int { cur(desc, index); return readIntValue() }
        override fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long { cur(desc, index); return readLongValue() }
        override fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float { cur(desc, index); return readFloatValue() }
        override fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double { cur(desc, index); return readDoubleValue() }
        override fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char { cur(desc, index); return readCharValue() }
        override fun readStringElementValue(desc: KSerialClassDesc, index: Int): String { cur(desc, index); return readStringValue() }

        override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T {
            cur(desc, index)
            return readEnumValue(enumClass)
        }

        override fun <T: Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T {
            cur(desc, index)
            return readSerializableValue(loader)
        }

        override fun <T: Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? {
            cur(desc, index)
            return readNullableSerializableValue(loader)
        }
    }
}
