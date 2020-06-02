package kotlinx.serialization

import kotlinx.serialization.builtins.AbstractEncoder
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.internal.BEGIN_LIST
import kotlinx.serialization.json.internal.BEGIN_OBJ
import kotlinx.serialization.json.internal.END_LIST
import kotlinx.serialization.json.internal.END_OBJ
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextualOrDefault
import kotlin.math.abs
import kotlin.math.floor


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
 *
 * @param encodeNullAsUndefined if true null properties will be omitted from the output
 */
public class DynamicObjectSerializer @OptIn(UnstableDefault::class) constructor(
    public val context: SerialModule = EmptyModule,
    private val configuration: JsonConfiguration = JsonConfiguration.Default,
    private val encodeNullAsUndefined: Boolean = false
) {

    public fun <T> serialize(strategy: SerializationStrategy<T>, obj: T): dynamic {
        if (strategy.descriptor.kind is PrimitiveKind || strategy.descriptor.kind is UnionKind.ENUM_KIND) {
            val serializer = DynamicPrimitiveEncoder(configuration)
            serializer.encode(strategy, obj)
            return serializer.result
        }
        val serializer = DynamicObjectEncoder(configuration, encodeNullAsUndefined)
        serializer.encode(strategy, obj)
        return serializer.result
    }

    public inline fun <reified T : Any> serialize(obj: T): dynamic =
        serialize(serializer(), obj)

    public inline fun <reified T : Any> serialize(obj: List<T?>): dynamic =
        serialize(serializer<T>().nullable.list, obj)
}

private class DynamicObjectEncoder(val configuration: JsonConfiguration, val encodeNullAsUndefined: Boolean) :
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

    fun selectMode(desc: SerialDescriptor) = when (desc.kind) {
        StructureKind.CLASS, StructureKind.OBJECT, UnionKind.CONTEXTUAL -> WriteMode.OBJ
        StructureKind.LIST, is PolymorphicKind -> WriteMode.LIST
        StructureKind.MAP -> WriteMode.MAP
        is PrimitiveKind, UnionKind.ENUM_KIND -> {
            // the two cases are handled in DynamicObjectSerializer. But compiler does not know
            error("DynamicObjectSerializer does not support serialization of singular primitive values or enum types.")
        }
    }
}

private class DynamicPrimitiveEncoder(private val configuration: JsonConfiguration) : AbstractEncoder() {
    var result: dynamic = null

    override fun encodeNull() {
        result = null
    }

    override fun encodeLong(value: Long) {
        val asDouble = value.toDouble()

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

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeValue(enumDescriptor.getElementName(index))
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}
