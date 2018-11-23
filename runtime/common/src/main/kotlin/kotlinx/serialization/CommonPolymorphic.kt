package kotlinx.serialization

import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KClass

// todo
object PolymorphicClassDesc : SerialClassDescImpl("kotlin.Any") {
    override val kind: SerialKind = UnionKind.POLYMORPHIC

    init {
        addElement("class")
        addElement("value")
    }
}

class SubtypeNotRegisteredException(subClassName: String, basePolyType: KClass<*>):
    SerializationException("$subClassName is not registered for polymorphic serialization in the scope of $basePolyType") {

    constructor(subClass: KClass<*>, basePolyType: KClass<*>): this(subClass.toString(), basePolyType)
}

@Suppress("UNCHECKED_CAST")
class PolymorphicSerializer<T : Any>(private val basePolyType: KClass<T>) : KSerializer<Any> {
    override fun serialize(output: Encoder, obj: Any) {
        val scopedSerial = output.context.resolveFromBase(basePolyType, obj as T)
                ?: throw SubtypeNotRegisteredException(obj::class, basePolyType)

        val out = output.beginStructure(descriptor)
        out.encodeStringElement(descriptor, 0, scopedSerial.descriptor.name)
        out.encodeSerializableElement(descriptor, 1, scopedSerial as KSerializer<Any>, obj)
        out.endStructure(descriptor)
    }

    override fun deserialize(input: Decoder): Any {
        @Suppress("NAME_SHADOWING")
        val input = input.beginStructure(descriptor)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_ALL -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                    val loader = input.context.resolveFromBase(basePolyType, klassName)
                            ?: throw SubtypeNotRegisteredException(klassName, basePolyType)
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                    break@mainLoop
                }
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = input.context.resolveFromBase(basePolyType, klassName)
                            ?: throw SubtypeNotRegisteredException(klassName, basePolyType)
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.endStructure(descriptor)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }

    override val descriptor: SerialDescriptor
        get() = PolymorphicClassDesc
}
