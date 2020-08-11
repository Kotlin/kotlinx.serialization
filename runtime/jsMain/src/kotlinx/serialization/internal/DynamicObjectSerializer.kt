/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.math.*


/**
 * Converts Kotlin data structures to plain Javascript objects
 *
 *
 * Limitations:
 * * Map keys must be of primitive or enum type
 * * Enums are serialized as the value of `@SerialName` if present or their name, in that order.
 * * Currently does not support polymorphism
 *
 * Example of usage:
 * ```
 *  @Serializable
 *  open class DataWrapper(open val s: String, val d: String?)
 *
 *  val wrapper = DataWrapper("foo", "bar")
 *  val plainJS: dynamic = DynamicObjectSerializer().serialize(DataWrapper.serializer(), wrapper)
 * ```
 */
internal class DynamicObjectSerializer(
    private val serializersModule: SerializersModule,
    private val configuration: JsonConf,
    private val encodeNullAsUndefined: Boolean
) {

    public fun <T> serialize(strategy: SerializationStrategy<T>, obj: T): dynamic {
        if (strategy.descriptor.kind is PrimitiveKind || strategy.descriptor.kind is SerialKind.ENUM) {
            val serializer = DynamicPrimitiveEncoder(serializersModule, configuration)
            serializer.encodeSerializableValue(strategy, obj)
            return serializer.result
        }
        val serializer = DynamicObjectEncoder(serializersModule, configuration, encodeNullAsUndefined)
        serializer.encodeSerializableValue(strategy, obj)
        return serializer.result
    }
}

private class DynamicObjectEncoder(
    override val serializersModule: SerializersModule,
    val configuration: JsonConf,
    val encodeNullAsUndefined: Boolean
) :
    AbstractEncoder() {
    private object NoOutputMark

    var result: dynamic = NoOutputMark
    lateinit var current: Node
    var currentName: String? = null
    lateinit var currentDescriptor: SerialDescriptor
    var currentElementIsMapKey = false

    class Node(val writeMode: WriteMode, val jsObject: dynamic) {
        var index: Int = 0
        lateinit var parent: Node
    }

    enum class WriteMode {
        OBJ, MAP, LIST
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        current.index = index
        currentDescriptor = descriptor

        if (current.writeMode == WriteMode.MAP) {
            currentElementIsMapKey = current.index % 2 == 0
        } else {
            currentName = descriptor.getElementName(index)
        }
        return true
    }

    override fun encodeValue(value: Any) {
        if (currentElementIsMapKey) {
            currentName = value.toString()
        } else {
            current.jsObject[currentName] = value
        }
    }

    override fun encodeChar(value: Char) {
        encodeValue(value.toString())
    }

    override fun encodeNull() {
        if (currentElementIsMapKey) {
            currentName = null
        } else {
            if (encodeNullAsUndefined) return // omit element

            current.jsObject[currentName] = null
        }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeValue(enumDescriptor.getElementName(index))
    }

    override fun encodeLong(value: Long) {
        val asDouble = value.toDouble()
        val conversionHasLossOfPrecision = abs(asDouble) > MAX_SAFE_INTEGER
        // todo: shall it be driven by isLenient or another configuration key?
        if (!configuration.isLenient && conversionHasLossOfPrecision) {
            throw IllegalArgumentException(
                "$value can't be serialized to number due to a potential precision loss. " +
                        "Use the JsonConfiguration option isLenient to serialize anyway"
            )
        }

        if (currentElementIsMapKey && conversionHasLossOfPrecision) {
            throw IllegalArgumentException(
                "Long with value $value can't be used in json as map key, because its value is larger than Number.MAX_SAFE_INTEGER"
            )
        }

        encodeValue(asDouble)

    }

    override fun encodeFloat(value: Float) {
        encodeDouble(value.toDouble())
    }

    override fun encodeDouble(value: Double) {
        if (currentElementIsMapKey) {
            val hasNonZeroFractionalPart = floor(value) != value
            if (!value.isFinite() || hasNonZeroFractionalPart) {
                throw IllegalArgumentException(
                    "Double with value $value can't be used in json as map key, because its value is not an integer."
                )
            }
        }
        encodeValue(value)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = configuration.encodeDefaults

    fun enterNode(jsObject: dynamic, writeMode: WriteMode) {
        val child = Node(writeMode, jsObject)
        child.parent = current
        current = child
    }

    fun exitNode() {
        current = current.parent
        currentElementIsMapKey = false
    }

    override fun beginStructure(
        descriptor: SerialDescriptor,
        vararg typeSerializers: KSerializer<*>
    ): CompositeEncoder {
        // we currently do not structures as map key
        if (currentElementIsMapKey) {
            throw IllegalArgumentException(
                "Value of type ${descriptor.serialName} can't be used in json as map key. " +
                        "It should have either primitive or enum kind, but its kind is ${descriptor.kind}."
            )
        }

        val newMode = selectMode(descriptor)
        if (result === NoOutputMark) {

            result = newChild(newMode)
            current = Node(newMode, result)
            current.parent = current
        } else {
            val child = newChild(newMode)
            current.jsObject[currentName] = child
            enterNode(child, newMode)
        }

        current.index = 0
        return this
    }

    fun newChild(writeMode: WriteMode) = when (writeMode) {
        WriteMode.OBJ, WriteMode.MAP -> js(BEGIN_OBJ.toString() + END_OBJ)
        WriteMode.LIST -> js(BEGIN_LIST.toString() + END_LIST)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        exitNode()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun selectMode(desc: SerialDescriptor) = when (desc.kind) {
        StructureKind.CLASS, StructureKind.OBJECT, SerialKind.CONTEXTUAL -> WriteMode.OBJ
        StructureKind.LIST, is PolymorphicKind -> WriteMode.LIST
        StructureKind.MAP -> WriteMode.MAP
        is PrimitiveKind, SerialKind.ENUM -> {
            // the two cases are handled in DynamicObjectSerializer. But compiler does not know
            error("DynamicObjectSerializer does not support serialization of singular primitive values or enum types.")
        }
    }
}

private class DynamicPrimitiveEncoder(
    override val serializersModule: SerializersModule,
    private val configuration: JsonConf
) : AbstractEncoder() {
    var result: dynamic = null

    override fun encodeNull() {
        result = null
    }

    override fun encodeLong(value: Long) {
        val asDouble = value.toDouble()
        // todo: shall it be driven by isLenient or another configuration key?
        if (!configuration.isLenient && abs(value) > MAX_SAFE_INTEGER) {
            throw IllegalArgumentException(
                "$value can't be deserialized to number due to a potential precision loss. " +
                        "Use the JsonConfiguration option isLenient to serialise anyway"
            )
        }
        encodeValue(asDouble)
    }

    override fun encodeChar(value: Char) {
        encodeValue(value.toString())
    }

    override fun encodeValue(value: Any) {
        result = value
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeValue(enumDescriptor.getElementName(index))
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}
